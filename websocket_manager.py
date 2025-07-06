"""
WebSocket Connection Manager for Real-time Golf Swing Analysis.

This module manages WebSocket connections for real-time golf swing analysis,
providing efficient connection handling, message routing, and session management
for live coaching and streaming feedback delivery.

Key Features:
- Connection lifecycle management with automatic cleanup
- Session-based connection grouping for multi-user support
- Message broadcasting and targeted delivery
- Performance monitoring and connection health checks
- Graceful error handling and reconnection support
- Memory-efficient connection pooling

Components:
- `ConnectionManager`: Main WebSocket connection management class
- `ConnectionInfo`: Data structure for connection metadata
- `SessionManager`: Manages coaching sessions and user groups
- `MessageRouter`: Routes messages based on session and user context
"""

import asyncio
import json
import time
import uuid
from typing import Dict, List, Set, Optional, Any, Callable
from dataclasses import dataclass, field
from enum import Enum
import logging
from fastapi import WebSocket, WebSocketDisconnect
from pydantic import BaseModel

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ConnectionStatus(Enum):
    CONNECTING = "connecting"
    CONNECTED = "connected"
    DISCONNECTING = "disconnecting"
    DISCONNECTED = "disconnected"
    ERROR = "error"

class MessageType(Enum):
    # Connection management
    CONNECT = "connect"
    DISCONNECT = "disconnect"
    PING = "ping"
    PONG = "pong"
    
    # Streaming data
    FRAME_DATA = "frame_data"
    ANALYSIS_RESULT = "analysis_result"
    FEEDBACK = "feedback"
    
    # Session management
    START_SESSION = "start_session"
    END_SESSION = "end_session"
    JOIN_SESSION = "join_session"
    LEAVE_SESSION = "leave_session"
    
    # Real-time KPIs
    KPI_UPDATE = "kpi_update"
    FAULT_DETECTED = "fault_detected"
    PERFORMANCE_METRICS = "performance_metrics"
    
    # Coaching
    COACHING_TIP = "coaching_tip"
    DRILL_SUGGESTION = "drill_suggestion"
    
    # Errors
    ERROR = "error"
    VALIDATION_ERROR = "validation_error"

@dataclass
class ConnectionInfo:
    """Metadata for WebSocket connections"""
    websocket: WebSocket
    user_id: str
    session_id: Optional[str] = None
    connected_at: float = field(default_factory=time.time)
    last_ping: float = field(default_factory=time.time)
    status: ConnectionStatus = ConnectionStatus.CONNECTING
    client_info: Dict[str, Any] = field(default_factory=dict)
    subscription_topics: Set[str] = field(default_factory=set)

class WebSocketMessage(BaseModel):
    """Standard WebSocket message format"""
    type: str
    data: Dict[str, Any] = {}
    timestamp: float = field(default_factory=time.time)
    message_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    session_id: Optional[str] = None
    user_id: Optional[str] = None

class SessionManager:
    """Manages coaching sessions and user groups"""
    
    def __init__(self):
        self.active_sessions: Dict[str, Dict[str, Any]] = {}
        self.session_connections: Dict[str, Set[str]] = {}  # session_id -> connection_ids
        self.user_sessions: Dict[str, str] = {}  # user_id -> session_id
    
    def create_session(self, session_id: str, creator_user_id: str, session_config: Dict[str, Any]) -> bool:
        """Create a new coaching session"""
        if session_id in self.active_sessions:
            return False
        
        self.active_sessions[session_id] = {
            "creator": creator_user_id,
            "created_at": time.time(),
            "config": session_config,
            "participants": {creator_user_id},
            "status": "active"
        }
        self.session_connections[session_id] = set()
        self.user_sessions[creator_user_id] = session_id
        
        logger.info(f"Created session {session_id} for user {creator_user_id}")
        return True
    
    def join_session(self, session_id: str, user_id: str, connection_id: str) -> bool:
        """Add user to existing session"""
        if session_id not in self.active_sessions:
            return False
        
        self.active_sessions[session_id]["participants"].add(user_id)
        self.session_connections[session_id].add(connection_id)
        self.user_sessions[user_id] = session_id
        
        logger.info(f"User {user_id} joined session {session_id}")
        return True
    
    def leave_session(self, user_id: str, connection_id: str) -> Optional[str]:
        """Remove user from their current session"""
        if user_id not in self.user_sessions:
            return None
        
        session_id = self.user_sessions[user_id]
        
        if session_id in self.active_sessions:
            self.active_sessions[session_id]["participants"].discard(user_id)
            
        if session_id in self.session_connections:
            self.session_connections[session_id].discard(connection_id)
        
        del self.user_sessions[user_id]
        
        # Clean up empty sessions
        if (session_id in self.active_sessions and 
            len(self.active_sessions[session_id]["participants"]) == 0):
            self.end_session(session_id)
        
        logger.info(f"User {user_id} left session {session_id}")
        return session_id
    
    def end_session(self, session_id: str) -> bool:
        """End a coaching session"""
        if session_id not in self.active_sessions:
            return False
        
        # Remove all users from session
        participants = self.active_sessions[session_id]["participants"].copy()
        for user_id in participants:
            if user_id in self.user_sessions:
                del self.user_sessions[user_id]
        
        # Clean up session data
        del self.active_sessions[session_id]
        if session_id in self.session_connections:
            del self.session_connections[session_id]
        
        logger.info(f"Ended session {session_id}")
        return True
    
    def get_session_connections(self, session_id: str) -> Set[str]:
        """Get all connection IDs for a session"""
        return self.session_connections.get(session_id, set())
    
    def get_user_session(self, user_id: str) -> Optional[str]:
        """Get current session ID for a user"""
        return self.user_sessions.get(user_id)

class MessageRouter:
    """Routes messages based on session and user context"""
    
    def __init__(self, connection_manager: 'ConnectionManager'):
        self.connection_manager = connection_manager
        self.message_handlers: Dict[str, Callable] = {}
    
    def register_handler(self, message_type: str, handler: Callable):
        """Register a message handler for a specific message type"""
        self.message_handlers[message_type] = handler
    
    async def route_message(self, connection_id: str, message: WebSocketMessage):
        """Route incoming message to appropriate handler"""
        handler = self.message_handlers.get(message.type)
        if handler:
            try:
                await handler(connection_id, message)
            except Exception as e:
                logger.error(f"Error handling message {message.type}: {e}")
                await self.connection_manager.send_error(
                    connection_id, 
                    f"Error processing {message.type}: {str(e)}"
                )
        else:
            logger.warning(f"No handler for message type: {message.type}")

class ConnectionManager:
    """Main WebSocket connection manager"""
    
    def __init__(self):
        self.connections: Dict[str, ConnectionInfo] = {}
        self.session_manager = SessionManager()
        self.message_router = MessageRouter(self)
        self.connection_stats = {
            "total_connections": 0,
            "active_connections": 0,
            "messages_sent": 0,
            "messages_received": 0,
            "errors": 0
        }
        
        # Register default message handlers
        self._register_default_handlers()
        
        # Start background tasks
        self._start_background_tasks()
    
    def _register_default_handlers(self):
        """Register default message handlers"""
        self.message_router.register_handler("ping", self._handle_ping)
        self.message_router.register_handler("start_session", self._handle_start_session)
        self.message_router.register_handler("join_session", self._handle_join_session)
        self.message_router.register_handler("leave_session", self._handle_leave_session)
        self.message_router.register_handler("end_session", self._handle_end_session)
    
    def _start_background_tasks(self):
        """Start background maintenance tasks"""
        asyncio.create_task(self._health_check_loop())
        asyncio.create_task(self._cleanup_loop())
    
    async def connect(self, websocket: WebSocket, user_id: str, client_info: Dict[str, Any] = None) -> str:
        """Accept new WebSocket connection"""
        await websocket.accept()
        
        connection_id = str(uuid.uuid4())
        connection_info = ConnectionInfo(
            websocket=websocket,
            user_id=user_id,
            client_info=client_info or {},
            status=ConnectionStatus.CONNECTED
        )
        
        self.connections[connection_id] = connection_info
        self.connection_stats["total_connections"] += 1
        self.connection_stats["active_connections"] += 1
        
        logger.info(f"New connection {connection_id} for user {user_id}")
        
        # Send connection confirmation
        await self.send_message(connection_id, WebSocketMessage(
            type=MessageType.CONNECT.value,
            data={"connection_id": connection_id, "status": "connected"}
        ))
        
        return connection_id
    
    async def disconnect(self, connection_id: str):
        """Disconnect WebSocket connection"""
        if connection_id not in self.connections:
            return
        
        connection_info = self.connections[connection_id]
        connection_info.status = ConnectionStatus.DISCONNECTING
        
        # Remove from session if applicable
        if connection_info.session_id:
            self.session_manager.leave_session(connection_info.user_id, connection_id)
        
        # Close WebSocket
        try:
            await connection_info.websocket.close()
        except Exception as e:
            logger.error(f"Error closing WebSocket {connection_id}: {e}")
        
        # Remove from connections
        del self.connections[connection_id]
        self.connection_stats["active_connections"] -= 1
        
        logger.info(f"Disconnected {connection_id} for user {connection_info.user_id}")
    
    async def send_message(self, connection_id: str, message: WebSocketMessage) -> bool:
        """Send message to specific connection"""
        if connection_id not in self.connections:
            return False
        
        connection_info = self.connections[connection_id]
        if connection_info.status != ConnectionStatus.CONNECTED:
            return False
        
        try:
            message_json = message.model_dump_json()
            await connection_info.websocket.send_text(message_json)
            self.connection_stats["messages_sent"] += 1
            return True
        except Exception as e:
            logger.error(f"Error sending message to {connection_id}: {e}")
            self.connection_stats["errors"] += 1
            await self.disconnect(connection_id)
            return False
    
    async def broadcast_to_session(self, session_id: str, message: WebSocketMessage) -> int:
        """Broadcast message to all connections in a session"""
        connection_ids = self.session_manager.get_session_connections(session_id)
        sent_count = 0
        
        for connection_id in connection_ids:
            if await self.send_message(connection_id, message):
                sent_count += 1
        
        return sent_count
    
    async def send_to_user(self, user_id: str, message: WebSocketMessage) -> bool:
        """Send message to specific user (first active connection)"""
        for connection_id, connection_info in self.connections.items():
            if (connection_info.user_id == user_id and 
                connection_info.status == ConnectionStatus.CONNECTED):
                return await self.send_message(connection_id, message)
        return False
    
    async def send_error(self, connection_id: str, error_message: str):
        """Send error message to connection"""
        error_msg = WebSocketMessage(
            type=MessageType.ERROR.value,
            data={"error": error_message, "timestamp": time.time()}
        )
        await self.send_message(connection_id, error_msg)
    
    async def receive_message(self, connection_id: str) -> Optional[WebSocketMessage]:
        """Receive and parse message from connection"""
        if connection_id not in self.connections:
            return None
        
        connection_info = self.connections[connection_id]
        
        try:
            data = await connection_info.websocket.receive_text()
            self.connection_stats["messages_received"] += 1
            
            # Parse JSON message
            message_data = json.loads(data)
            message = WebSocketMessage(**message_data)
            
            # Update connection info
            connection_info.last_ping = time.time()
            
            # Route message
            await self.message_router.route_message(connection_id, message)
            
            return message
            
        except WebSocketDisconnect:
            await self.disconnect(connection_id)
            return None
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON from {connection_id}: {e}")
            await self.send_error(connection_id, "Invalid JSON format")
            return None
        except Exception as e:
            logger.error(f"Error receiving message from {connection_id}: {e}")
            self.connection_stats["errors"] += 1
            return None
    
    # Message Handlers
    async def _handle_ping(self, connection_id: str, message: WebSocketMessage):
        """Handle ping message"""
        pong_message = WebSocketMessage(
            type=MessageType.PONG.value,
            data={"timestamp": time.time()}
        )
        await self.send_message(connection_id, pong_message)
    
    async def _handle_start_session(self, connection_id: str, message: WebSocketMessage):
        """Handle start session request"""
        if connection_id not in self.connections:
            return
        
        connection_info = self.connections[connection_id]
        session_id = message.data.get("session_id", str(uuid.uuid4()))
        session_config = message.data.get("config", {})
        
        if self.session_manager.create_session(session_id, connection_info.user_id, session_config):
            connection_info.session_id = session_id
            self.session_manager.join_session(session_id, connection_info.user_id, connection_id)
            
            response = WebSocketMessage(
                type="session_started",
                data={"session_id": session_id, "status": "created"}
            )
        else:
            response = WebSocketMessage(
                type=MessageType.ERROR.value,
                data={"error": "Failed to create session", "session_id": session_id}
            )
        
        await self.send_message(connection_id, response)
    
    async def _handle_join_session(self, connection_id: str, message: WebSocketMessage):
        """Handle join session request"""
        if connection_id not in self.connections:
            return
        
        connection_info = self.connections[connection_id]
        session_id = message.data.get("session_id")
        
        if not session_id:
            await self.send_error(connection_id, "session_id required")
            return
        
        if self.session_manager.join_session(session_id, connection_info.user_id, connection_id):
            connection_info.session_id = session_id
            
            response = WebSocketMessage(
                type="session_joined",
                data={"session_id": session_id, "status": "joined"}
            )
        else:
            response = WebSocketMessage(
                type=MessageType.ERROR.value,
                data={"error": "Failed to join session", "session_id": session_id}
            )
        
        await self.send_message(connection_id, response)
    
    async def _handle_leave_session(self, connection_id: str, message: WebSocketMessage):
        """Handle leave session request"""
        if connection_id not in self.connections:
            return
        
        connection_info = self.connections[connection_id]
        session_id = self.session_manager.leave_session(connection_info.user_id, connection_id)
        
        if session_id:
            connection_info.session_id = None
            response = WebSocketMessage(
                type="session_left",
                data={"session_id": session_id, "status": "left"}
            )
        else:
            response = WebSocketMessage(
                type=MessageType.ERROR.value,
                data={"error": "Not in any session"}
            )
        
        await self.send_message(connection_id, response)
    
    async def _handle_end_session(self, connection_id: str, message: WebSocketMessage):
        """Handle end session request"""
        if connection_id not in self.connections:
            return
        
        connection_info = self.connections[connection_id]
        session_id = message.data.get("session_id")
        
        if not session_id:
            session_id = connection_info.session_id
        
        if session_id and self.session_manager.end_session(session_id):
            # Notify all connections in session
            end_message = WebSocketMessage(
                type="session_ended",
                data={"session_id": session_id, "status": "ended"}
            )
            await self.broadcast_to_session(session_id, end_message)
            
            # Clear session from connections
            for conn_id, conn_info in self.connections.items():
                if conn_info.session_id == session_id:
                    conn_info.session_id = None
        else:
            await self.send_error(connection_id, "Failed to end session")
    
    # Background Tasks
    async def _health_check_loop(self):
        """Periodically check connection health"""
        while True:
            try:
                await asyncio.sleep(30)  # Check every 30 seconds
                
                current_time = time.time()
                stale_connections = []
                
                for connection_id, connection_info in self.connections.items():
                    # Check for stale connections (no ping in 60 seconds)
                    if current_time - connection_info.last_ping > 60:
                        stale_connections.append(connection_id)
                
                # Disconnect stale connections
                for connection_id in stale_connections:
                    logger.info(f"Disconnecting stale connection: {connection_id}")
                    await self.disconnect(connection_id)
                
            except Exception as e:
                logger.error(f"Error in health check loop: {e}")
    
    async def _cleanup_loop(self):
        """Periodically clean up resources"""
        while True:
            try:
                await asyncio.sleep(300)  # Clean up every 5 minutes
                
                # Log connection statistics
                logger.info(f"Connection stats: {self.connection_stats}")
                
                # Additional cleanup logic can be added here
                
            except Exception as e:
                logger.error(f"Error in cleanup loop: {e}")
    
    def get_connection_stats(self) -> Dict[str, Any]:
        """Get connection statistics"""
        return {
            **self.connection_stats,
            "active_sessions": len(self.session_manager.active_sessions),
            "connections_by_status": {
                status.value: sum(1 for conn in self.connections.values() 
                                if conn.status == status)
                for status in ConnectionStatus
            }
        }

# Global connection manager instance
connection_manager = ConnectionManager()