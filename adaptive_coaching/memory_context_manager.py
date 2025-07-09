"""
Memory and Context Management System - Intelligent Session Continuity

This module implements sophisticated memory and context management to ensure
seamless continuity across coaching sessions. It remembers everything important
about each user's journey, maintains conversation context, and provides
intelligent recall of relevant information.

Key Features:
- Long-term memory storage and retrieval
- Context-aware conversation management
- Intelligent information prioritization
- Cross-session continuity
- Semantic memory organization
- Temporal context tracking
- Goal and progress memory
- Relationship and pattern memory

The system ensures users feel like they're talking to a coach who truly
remembers their golf journey and builds on every interaction.
"""

import json
import logging
import sqlite3
import pickle
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field, asdict
from enum import Enum
import numpy as np
from collections import defaultdict, deque
import hashlib

logger = logging.getLogger(__name__)

class MemoryType(Enum):
    """Types of memory stored by the system"""
    CONVERSATIONAL = "conversational"
    FACTUAL = "factual"
    PROCEDURAL = "procedural"
    EPISODIC = "episodic"
    SEMANTIC = "semantic"
    EMOTIONAL = "emotional"

class MemoryImportance(Enum):
    """Importance levels for memory items"""
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"

class ContextScope(Enum):
    """Scope of context information"""
    SESSION = "session"
    DAILY = "daily"
    WEEKLY = "weekly"
    MONTHLY = "monthly"
    PERMANENT = "permanent"

@dataclass
class MemoryItem:
    """Individual memory item"""
    memory_id: str
    user_id: str
    memory_type: MemoryType
    
    # Content
    content: str
    structured_data: Dict[str, Any] = field(default_factory=dict)
    
    # Metadata
    importance: MemoryImportance = MemoryImportance.MEDIUM
    confidence: float = 1.0
    
    # Temporal information
    created_at: datetime = field(default_factory=datetime.now)
    last_accessed: datetime = field(default_factory=datetime.now)
    expires_at: Optional[datetime] = None
    
    # Associations
    tags: List[str] = field(default_factory=list)
    related_memories: List[str] = field(default_factory=list)
    context_keys: List[str] = field(default_factory=list)
    
    # Usage tracking
    access_count: int = 0
    relevance_score: float = 1.0

@dataclass
class ConversationContext:
    """Context for a specific conversation session"""
    session_id: str
    user_id: str
    
    # Current state
    current_topic: str = ""
    emotional_state: str = "neutral"
    engagement_level: float = 0.5
    
    # Conversation flow
    message_history: List[Dict[str, Any]] = field(default_factory=list)
    topic_history: List[str] = field(default_factory=list)
    intent_history: List[str] = field(default_factory=list)
    
    # Session metadata
    started_at: datetime = field(default_factory=datetime.now)
    last_activity: datetime = field(default_factory=datetime.now)
    session_duration: float = 0.0
    
    # Performance context
    recent_swing_data: Dict[str, Any] = field(default_factory=dict)
    session_goals: List[str] = field(default_factory=list)
    achievements_this_session: List[str] = field(default_factory=list)
    
    # Coaching context
    coaching_approach: str = "balanced"
    effectiveness_feedback: List[float] = field(default_factory=list)
    user_preferences: Dict[str, Any] = field(default_factory=dict)

@dataclass
class UserContext:
    """Comprehensive user context across all sessions"""
    user_id: str
    
    # Identity and preferences
    user_profile: Dict[str, Any] = field(default_factory=dict)
    learning_preferences: Dict[str, Any] = field(default_factory=dict)
    communication_style: Dict[str, Any] = field(default_factory=dict)
    
    # Goals and progress
    active_goals: List[Dict[str, Any]] = field(default_factory=list)
    completed_goals: List[Dict[str, Any]] = field(default_factory=list)
    long_term_objectives: List[str] = field(default_factory=list)
    
    # Performance patterns
    skill_progression: Dict[str, float] = field(default_factory=dict)
    common_challenges: List[str] = field(default_factory=list)
    breakthrough_moments: List[Dict[str, Any]] = field(default_factory=list)
    
    # Relationship context
    coaching_relationship_stage: str = "building"  # building, established, advanced
    trust_level: float = 0.5
    communication_rapport: float = 0.5
    
    # Temporal context
    created_at: datetime = field(default_factory=datetime.now)
    last_updated: datetime = field(default_factory=datetime.now)
    total_sessions: int = 0
    total_interaction_time: float = 0.0

class MemoryContextManager:
    """Main memory and context management system"""
    
    def __init__(self, db_path: str = "memory_context.db"):
        self.db_path = db_path
        
        # Memory storage
        self.memories: Dict[str, List[MemoryItem]] = defaultdict(list)
        self.memory_index: Dict[str, List[str]] = defaultdict(list)  # Tag -> memory_ids
        
        # Context storage
        self.conversation_contexts: Dict[str, ConversationContext] = {}
        self.user_contexts: Dict[str, UserContext] = {}
        
        # Memory management parameters
        self.max_memories_per_user = 1000
        self.memory_decay_factor = 0.1
        self.relevance_threshold = 0.3
        
        # Initialize database
        self._init_database()
        
        # Load existing data
        self._load_data()
        
        logger.info("Memory Context Manager initialized")
    
    def _init_database(self):
        """Initialize SQLite database for persistent storage"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # Memory items table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS memory_items (
                memory_id TEXT PRIMARY KEY,
                user_id TEXT,
                memory_type TEXT,
                content TEXT,
                structured_data BLOB,
                importance TEXT,
                confidence REAL,
                created_at TIMESTAMP,
                last_accessed TIMESTAMP,
                expires_at TIMESTAMP,
                tags TEXT,
                related_memories TEXT,
                context_keys TEXT,
                access_count INTEGER,
                relevance_score REAL
            )
        ''')
        
        # Conversation contexts table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS conversation_contexts (
                session_id TEXT PRIMARY KEY,
                user_id TEXT,
                context_data BLOB,
                created_at TIMESTAMP,
                last_updated TIMESTAMP
            )
        ''')
        
        # User contexts table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_contexts (
                user_id TEXT PRIMARY KEY,
                context_data BLOB,
                created_at TIMESTAMP,
                last_updated TIMESTAMP
            )
        ''')
        
        # Memory associations table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS memory_associations (
                association_id TEXT PRIMARY KEY,
                memory_id_1 TEXT,
                memory_id_2 TEXT,
                association_strength REAL,
                association_type TEXT,
                created_at TIMESTAMP
            )
        ''')
        
        conn.commit()
        conn.close()
    
    def _load_data(self):
        """Load existing data from database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # Load memory items
        cursor.execute('SELECT * FROM memory_items')
        for row in cursor.fetchall():
            try:
                memory = self._row_to_memory_item(row)
                self.memories[memory.user_id].append(memory)
                
                # Update index
                for tag in memory.tags:
                    self.memory_index[tag].append(memory.memory_id)
                    
            except Exception as e:
                logger.error(f"Error loading memory item: {e}")
        
        # Load user contexts
        cursor.execute('SELECT user_id, context_data FROM user_contexts')
        for user_id, context_data in cursor.fetchall():
            try:
                context = pickle.loads(context_data)
                self.user_contexts[user_id] = context
            except Exception as e:
                logger.error(f"Error loading user context for {user_id}: {e}")
        
        conn.close()
        logger.info(f"Loaded data for {len(self.user_contexts)} users")
    
    def _row_to_memory_item(self, row) -> MemoryItem:
        """Convert database row to MemoryItem"""
        return MemoryItem(
            memory_id=row[0],
            user_id=row[1],
            memory_type=MemoryType(row[2]),
            content=row[3],
            structured_data=pickle.loads(row[4]) if row[4] else {},
            importance=MemoryImportance(row[5]),
            confidence=row[6],
            created_at=datetime.fromisoformat(row[7]),
            last_accessed=datetime.fromisoformat(row[8]),
            expires_at=datetime.fromisoformat(row[9]) if row[9] else None,
            tags=json.loads(row[10]) if row[10] else [],
            related_memories=json.loads(row[11]) if row[11] else [],
            context_keys=json.loads(row[12]) if row[12] else [],
            access_count=row[13],
            relevance_score=row[14]
        )
    
    def store_memory(self, user_id: str, content: str, memory_type: MemoryType,
                    importance: MemoryImportance = MemoryImportance.MEDIUM,
                    structured_data: Dict = None, tags: List[str] = None,
                    context_keys: List[str] = None) -> str:
        """Store a new memory item"""
        
        memory_id = self._generate_memory_id(user_id, content)
        
        memory = MemoryItem(
            memory_id=memory_id,
            user_id=user_id,
            memory_type=memory_type,
            content=content,
            structured_data=structured_data or {},
            importance=importance,
            tags=tags or [],
            context_keys=context_keys or []
        )
        
        # Store in memory
        self.memories[user_id].append(memory)
        
        # Update index
        for tag in memory.tags:
            self.memory_index[tag].append(memory_id)
        
        # Save to database
        self._save_memory_item(memory)
        
        # Manage memory size
        self._manage_memory_size(user_id)
        
        logger.debug(f"Stored memory {memory_id} for user {user_id}")
        return memory_id
    
    def _generate_memory_id(self, user_id: str, content: str) -> str:
        """Generate unique memory ID"""
        content_hash = hashlib.md5(content.encode()).hexdigest()[:8]
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        return f"{user_id}_{timestamp}_{content_hash}"
    
    def _save_memory_item(self, memory: MemoryItem):
        """Save memory item to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO memory_items 
            (memory_id, user_id, memory_type, content, structured_data, importance,
             confidence, created_at, last_accessed, expires_at, tags, related_memories,
             context_keys, access_count, relevance_score)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            memory.memory_id,
            memory.user_id,
            memory.memory_type.value,
            memory.content,
            pickle.dumps(memory.structured_data),
            memory.importance.value,
            memory.confidence,
            memory.created_at.isoformat(),
            memory.last_accessed.isoformat(),
            memory.expires_at.isoformat() if memory.expires_at else None,
            json.dumps(memory.tags),
            json.dumps(memory.related_memories),
            json.dumps(memory.context_keys),
            memory.access_count,
            memory.relevance_score
        ))
        
        conn.commit()
        conn.close()
    
    def recall_memories(self, user_id: str, query: str = "", 
                       memory_types: List[MemoryType] = None,
                       tags: List[str] = None, limit: int = 10) -> List[MemoryItem]:
        """Recall relevant memories for a user"""
        
        user_memories = self.memories.get(user_id, [])
        
        if not user_memories:
            return []
        
        # Filter by type
        if memory_types:
            user_memories = [m for m in user_memories if m.memory_type in memory_types]
        
        # Filter by tags
        if tags:
            user_memories = [m for m in user_memories if any(tag in m.tags for tag in tags)]
        
        # Score memories by relevance
        scored_memories = []
        for memory in user_memories:
            relevance_score = self._calculate_memory_relevance(memory, query)
            scored_memories.append((memory, relevance_score))
        
        # Sort by relevance and recency
        scored_memories.sort(key=lambda x: (x[1], x[0].last_accessed), reverse=True)
        
        # Update access information
        relevant_memories = []
        for memory, score in scored_memories[:limit]:
            if score > self.relevance_threshold:
                memory.last_accessed = datetime.now()
                memory.access_count += 1
                relevant_memories.append(memory)
                self._save_memory_item(memory)
        
        return relevant_memories
    
    def _calculate_memory_relevance(self, memory: MemoryItem, query: str) -> float:
        """Calculate relevance score for a memory item"""
        
        relevance = 0.0
        
        # Content similarity (simple keyword matching)
        if query:
            query_words = query.lower().split()
            content_words = memory.content.lower().split()
            matches = sum(1 for word in query_words if word in content_words)
            content_similarity = matches / max(len(query_words), 1)
            relevance += content_similarity * 0.4
        
        # Importance weighting
        importance_weights = {
            MemoryImportance.CRITICAL: 1.0,
            MemoryImportance.HIGH: 0.8,
            MemoryImportance.MEDIUM: 0.6,
            MemoryImportance.LOW: 0.4
        }
        relevance += importance_weights[memory.importance] * 0.3
        
        # Recency weighting
        days_since_access = (datetime.now() - memory.last_accessed).days
        recency_score = max(0, 1 - days_since_access / 30)  # Decay over 30 days
        relevance += recency_score * 0.2
        
        # Access frequency weighting
        frequency_score = min(1.0, memory.access_count / 10)  # Normalize to 10 accesses
        relevance += frequency_score * 0.1
        
        return min(1.0, relevance)
    
    def create_conversation_context(self, session_id: str, user_id: str) -> ConversationContext:
        """Create new conversation context"""
        
        context = ConversationContext(
            session_id=session_id,
            user_id=user_id
        )
        
        # Load user preferences and history
        user_context = self.get_user_context(user_id)
        if user_context:
            context.user_preferences = user_context.learning_preferences
            context.coaching_approach = user_context.communication_style.get("preferred_approach", "balanced")
        
        self.conversation_contexts[session_id] = context
        return context
    
    def update_conversation_context(self, session_id: str, **updates):
        """Update conversation context with new information"""
        
        if session_id not in self.conversation_contexts:
            logger.warning(f"Session {session_id} not found in contexts")
            return
        
        context = self.conversation_contexts[session_id]
        
        # Update fields
        for key, value in updates.items():
            if hasattr(context, key):
                setattr(context, key, value)
        
        context.last_activity = datetime.now()
        context.session_duration = (context.last_activity - context.started_at).total_seconds() / 60
        
        # Save to database
        self._save_conversation_context(context)
    
    def add_conversation_message(self, session_id: str, role: str, content: str, 
                               metadata: Dict = None):
        """Add message to conversation context"""
        
        if session_id not in self.conversation_contexts:
            return
        
        context = self.conversation_contexts[session_id]
        
        message = {
            "role": role,
            "content": content,
            "timestamp": datetime.now().isoformat(),
            "metadata": metadata or {}
        }
        
        context.message_history.append(message)
        
        # Extract and store important information
        if role == "user":
            self._extract_user_information(context, content, metadata)
        elif role == "assistant":
            self._extract_coaching_information(context, content, metadata)
        
        # Update context
        self.update_conversation_context(session_id, last_activity=datetime.now())
    
    def _extract_user_information(self, context: ConversationContext, content: str, metadata: Dict):
        """Extract important information from user messages"""
        
        # Extract emotional state
        emotional_indicators = {
            "frustrated": ["frustrated", "annoyed", "angry"],
            "excited": ["excited", "great", "awesome"],
            "confused": ["confused", "don't understand", "unclear"],
            "confident": ["confident", "ready", "good"]
        }
        
        for emotion, indicators in emotional_indicators.items():
            if any(indicator in content.lower() for indicator in indicators):
                context.emotional_state = emotion
                break
        
        # Extract goals or intentions
        goal_indicators = ["want to", "goal", "hope to", "trying to"]
        if any(indicator in content.lower() for indicator in goal_indicators):
            # Store as potential goal
            self.store_memory(
                context.user_id,
                f"User expressed goal: {content}",
                MemoryType.SEMANTIC,
                MemoryImportance.HIGH,
                tags=["goal", "intention"],
                context_keys=[context.session_id]
            )
        
        # Extract preferences
        preference_indicators = ["prefer", "like", "don't like", "hate"]
        if any(indicator in content.lower() for indicator in preference_indicators):
            self.store_memory(
                context.user_id,
                f"User preference: {content}",
                MemoryType.FACTUAL,
                MemoryImportance.MEDIUM,
                tags=["preference"],
                context_keys=[context.session_id]
            )
    
    def _extract_coaching_information(self, context: ConversationContext, content: str, metadata: Dict):
        """Extract important information from coaching responses"""
        
        # Track coaching effectiveness
        if metadata.get("user_response_positive"):
            context.effectiveness_feedback.append(1.0)
        elif metadata.get("user_response_negative"):
            context.effectiveness_feedback.append(0.0)
        
        # Store important coaching moments
        if metadata.get("breakthrough_moment"):
            self.store_memory(
                context.user_id,
                f"Breakthrough moment: {content}",
                MemoryType.EPISODIC,
                MemoryImportance.CRITICAL,
                tags=["breakthrough", "success"],
                context_keys=[context.session_id]
            )
    
    def get_conversation_context(self, session_id: str) -> Optional[ConversationContext]:
        """Get conversation context for a session"""
        return self.conversation_contexts.get(session_id)
    
    def get_user_context(self, user_id: str) -> UserContext:
        """Get or create user context"""
        
        if user_id not in self.user_contexts:
            self.user_contexts[user_id] = UserContext(user_id=user_id)
        
        return self.user_contexts[user_id]
    
    def update_user_context(self, user_id: str, **updates):
        """Update user context with new information"""
        
        context = self.get_user_context(user_id)
        
        # Update fields
        for key, value in updates.items():
            if hasattr(context, key):
                setattr(context, key, value)
        
        context.last_updated = datetime.now()
        
        # Save to database
        self._save_user_context(context)
    
    def _save_conversation_context(self, context: ConversationContext):
        """Save conversation context to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO conversation_contexts 
            (session_id, user_id, context_data, created_at, last_updated)
            VALUES (?, ?, ?, ?, ?)
        ''', (
            context.session_id,
            context.user_id,
            pickle.dumps(context),
            context.started_at.isoformat(),
            context.last_activity.isoformat()
        ))
        
        conn.commit()
        conn.close()
    
    def _save_user_context(self, context: UserContext):
        """Save user context to database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO user_contexts 
            (user_id, context_data, created_at, last_updated)
            VALUES (?, ?, ?, ?)
        ''', (
            context.user_id,
            pickle.dumps(context),
            context.created_at.isoformat(),
            context.last_updated.isoformat()
        ))
        
        conn.commit()
        conn.close()
    
    def get_contextual_information(self, user_id: str, session_id: str = None) -> Dict[str, Any]:
        """Get relevant contextual information for coaching"""
        
        # Get user context
        user_context = self.get_user_context(user_id)
        
        # Get conversation context if available
        conversation_context = None
        if session_id:
            conversation_context = self.get_conversation_context(session_id)
        
        # Recall relevant memories
        recent_memories = self.recall_memories(user_id, limit=5)
        
        contextual_info = {
            "user_profile": {
                "total_sessions": user_context.total_sessions,
                "coaching_relationship_stage": user_context.coaching_relationship_stage,
                "trust_level": user_context.trust_level,
                "learning_preferences": user_context.learning_preferences
            },
            "current_session": {},
            "relevant_memories": [
                {
                    "content": memory.content,
                    "type": memory.memory_type.value,
                    "importance": memory.importance.value,
                    "tags": memory.tags
                }
                for memory in recent_memories
            ],
            "active_goals": user_context.active_goals,
            "recent_achievements": []
        }
        
        # Add session context if available
        if conversation_context:
            contextual_info["current_session"] = {
                "emotional_state": conversation_context.emotional_state,
                "engagement_level": conversation_context.engagement_level,
                "current_topic": conversation_context.current_topic,
                "session_duration": conversation_context.session_duration,
                "coaching_approach": conversation_context.coaching_approach
            }
        
        return contextual_info
    
    def _manage_memory_size(self, user_id: str):
        """Manage memory size by removing least important/relevant memories"""
        
        user_memories = self.memories.get(user_id, [])
        
        if len(user_memories) <= self.max_memories_per_user:
            return
        
        # Calculate retention scores
        scored_memories = []
        for memory in user_memories:
            retention_score = self._calculate_retention_score(memory)
            scored_memories.append((memory, retention_score))
        
        # Sort by retention score
        scored_memories.sort(key=lambda x: x[1], reverse=True)
        
        # Keep top memories
        memories_to_keep = [memory for memory, score in scored_memories[:self.max_memories_per_user]]
        memories_to_remove = [memory for memory, score in scored_memories[self.max_memories_per_user:]]
        
        # Update memory list
        self.memories[user_id] = memories_to_keep
        
        # Remove from database
        for memory in memories_to_remove:
            self._remove_memory_from_db(memory.memory_id)
        
        logger.info(f"Cleaned up {len(memories_to_remove)} memories for user {user_id}")
    
    def _calculate_retention_score(self, memory: MemoryItem) -> float:
        """Calculate score for retaining a memory"""
        
        score = 0.0
        
        # Importance weighting
        importance_weights = {
            MemoryImportance.CRITICAL: 1.0,
            MemoryImportance.HIGH: 0.8,
            MemoryImportance.MEDIUM: 0.6,
            MemoryImportance.LOW: 0.4
        }
        score += importance_weights[memory.importance] * 0.4
        
        # Access frequency
        score += min(1.0, memory.access_count / 10) * 0.3
        
        # Recency
        days_old = (datetime.now() - memory.created_at).days
        recency_score = max(0, 1 - days_old / 90)  # Decay over 90 days
        score += recency_score * 0.2
        
        # Relevance
        score += memory.relevance_score * 0.1
        
        return score
    
    def _remove_memory_from_db(self, memory_id: str):
        """Remove memory from database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('DELETE FROM memory_items WHERE memory_id = ?', (memory_id,))
        
        conn.commit()
        conn.close()
    
    def create_memory_association(self, memory_id_1: str, memory_id_2: str, 
                                 association_strength: float, association_type: str):
        """Create association between memories"""
        
        association_id = f"{memory_id_1}_{memory_id_2}_{association_type}"
        
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT OR REPLACE INTO memory_associations
            (association_id, memory_id_1, memory_id_2, association_strength, 
             association_type, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (
            association_id, memory_id_1, memory_id_2, association_strength,
            association_type, datetime.now().isoformat()
        ))
        
        conn.commit()
        conn.close()
    
    def get_memory_insights(self, user_id: str) -> Dict[str, Any]:
        """Get insights about user's memory and context"""
        
        user_memories = self.memories.get(user_id, [])
        user_context = self.get_user_context(user_id)
        
        insights = {
            "memory_summary": {
                "total_memories": len(user_memories),
                "memory_types": {},
                "importance_distribution": {},
                "average_relevance": 0.0
            },
            "context_summary": {
                "total_sessions": user_context.total_sessions,
                "relationship_stage": user_context.coaching_relationship_stage,
                "trust_level": user_context.trust_level,
                "active_goals": len(user_context.active_goals)
            },
            "recent_activity": [],
            "memory_health": "good"
        }
        
        if user_memories:
            # Memory type distribution
            type_counts = defaultdict(int)
            importance_counts = defaultdict(int)
            total_relevance = 0
            
            for memory in user_memories:
                type_counts[memory.memory_type.value] += 1
                importance_counts[memory.importance.value] += 1
                total_relevance += memory.relevance_score
            
            insights["memory_summary"]["memory_types"] = dict(type_counts)
            insights["memory_summary"]["importance_distribution"] = dict(importance_counts)
            insights["memory_summary"]["average_relevance"] = total_relevance / len(user_memories)
            
            # Recent activity
            recent_memories = sorted(user_memories, key=lambda m: m.last_accessed, reverse=True)[:5]
            insights["recent_activity"] = [
                {
                    "content": memory.content[:100] + "..." if len(memory.content) > 100 else memory.content,
                    "type": memory.memory_type.value,
                    "last_accessed": memory.last_accessed.isoformat()
                }
                for memory in recent_memories
            ]
        
        return insights