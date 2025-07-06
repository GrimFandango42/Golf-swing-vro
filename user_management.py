"""
User authentication and profile management for SwingSync AI.

This module provides:
- User registration and authentication
- JWT token generation and validation
- Password hashing and verification
- User profile management
- Session management
- Security utilities

Dependencies:
- python-jose for JWT handling
- passlib for password hashing
- SQLAlchemy for database operations
"""

import os
from datetime import datetime, timedelta, timezone
from typing import Optional, Dict, Any, Union
from fastapi import Depends, HTTPException, status, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
from passlib.context import CryptContext
from sqlalchemy.orm import Session
from pydantic import BaseModel, EmailStr, Field
import uuid

from database import (
    User, UserPreferences, SessionLocal, get_db,
    get_user_by_email, get_user_by_username, SkillLevel
)

# Security configuration
SECRET_KEY = os.getenv("SECRET_KEY", "your-super-secret-key-change-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "30"))
REFRESH_TOKEN_EXPIRE_DAYS = int(os.getenv("REFRESH_TOKEN_EXPIRE_DAYS", "7"))

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# HTTP Bearer token scheme
security = HTTPBearer(auto_error=False)

# Pydantic models for API requests/responses

class UserRegistration(BaseModel):
    """User registration request model."""
    email: EmailStr
    username: str = Field(..., min_length=3, max_length=50)
    password: str = Field(..., min_length=8, max_length=100)
    first_name: Optional[str] = Field(None, max_length=100)
    last_name: Optional[str] = Field(None, max_length=100)
    skill_level: Optional[SkillLevel] = SkillLevel.BEGINNER
    handicap: Optional[float] = Field(None, ge=0, le=54)
    preferred_hand: Optional[str] = Field(None, regex="^(left|right)$")
    height_cm: Optional[float] = Field(None, gt=0, le=300)
    weight_kg: Optional[float] = Field(None, gt=0, le=500)

class UserLogin(BaseModel):
    """User login request model."""
    username_or_email: str
    password: str

class UserProfile(BaseModel):
    """User profile response model."""
    id: str
    email: str
    username: str
    first_name: Optional[str]
    last_name: Optional[str]
    skill_level: Optional[SkillLevel]
    handicap: Optional[float]
    preferred_hand: Optional[str]
    height_cm: Optional[float]
    weight_kg: Optional[float]
    is_active: bool
    is_verified: bool
    created_at: datetime
    last_login: Optional[datetime]

    class Config:
        from_attributes = True

class UserProfileUpdate(BaseModel):
    """User profile update request model."""
    first_name: Optional[str] = Field(None, max_length=100)
    last_name: Optional[str] = Field(None, max_length=100)
    skill_level: Optional[SkillLevel] = None
    handicap: Optional[float] = Field(None, ge=0, le=54)
    preferred_hand: Optional[str] = Field(None, regex="^(left|right)$")
    height_cm: Optional[float] = Field(None, gt=0, le=300)
    weight_kg: Optional[float] = Field(None, gt=0, le=500)

class UserPreferencesModel(BaseModel):
    """User preferences model."""
    preferred_units: str = Field("metric", regex="^(metric|imperial)$")
    feedback_detail_level: str = Field("detailed", regex="^(basic|detailed|advanced)$")
    focus_areas: Optional[list] = None
    email_notifications: bool = True
    push_notifications: bool = True
    weekly_reports: bool = True
    target_handicap: Optional[float] = Field(None, ge=0, le=54)
    primary_goals: Optional[list] = None
    share_data_for_research: bool = False
    public_profile: bool = False

class Token(BaseModel):
    """Token response model."""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int

class TokenData(BaseModel):
    """Token data for validation."""
    user_id: Optional[str] = None
    username: Optional[str] = None

# Password utilities

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash."""
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    """Generate password hash."""
    return pwd_context.hash(password)

# JWT utilities

def create_access_token(data: Dict[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    """Create JWT access token."""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode.update({"exp": expire, "type": "access"})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def create_refresh_token(data: Dict[str, Any]) -> str:
    """Create JWT refresh token."""
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)
    to_encode.update({"exp": expire, "type": "refresh"})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def verify_token(token: str, token_type: str = "access") -> Optional[TokenData]:
    """Verify and decode JWT token."""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        
        # Check token type
        if payload.get("type") != token_type:
            return None
            
        user_id: str = payload.get("sub")
        username: str = payload.get("username")
        
        if user_id is None:
            return None
            
        token_data = TokenData(user_id=user_id, username=username)
        return token_data
    except JWTError:
        return None

# Authentication functions

def authenticate_user(db: Session, username_or_email: str, password: str) -> Optional[User]:
    """Authenticate user with username/email and password."""
    # Try to find user by email first, then by username
    user = get_user_by_email(db, username_or_email)
    if not user:
        user = get_user_by_username(db, username_or_email)
    
    if not user:
        return None
    
    if not verify_password(password, user.hashed_password):
        return None
    
    return user

def create_user(db: Session, user_data: UserRegistration) -> User:
    """Create a new user."""
    # Check if user already exists
    if get_user_by_email(db, user_data.email):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )
    
    if get_user_by_username(db, user_data.username):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username already taken"
        )
    
    # Create user
    hashed_password = get_password_hash(user_data.password)
    db_user = User(
        id=str(uuid.uuid4()),
        email=user_data.email,
        username=user_data.username,
        hashed_password=hashed_password,
        first_name=user_data.first_name,
        last_name=user_data.last_name,
        skill_level=user_data.skill_level,
        handicap=user_data.handicap,
        preferred_hand=user_data.preferred_hand,
        height_cm=user_data.height_cm,
        weight_kg=user_data.weight_kg
    )
    
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    
    # Create default preferences
    preferences = UserPreferences(
        user_id=db_user.id,
        preferred_units="metric",
        feedback_detail_level="detailed",
        email_notifications=True,
        push_notifications=True,
        weekly_reports=True
    )
    
    db.add(preferences)
    db.commit()
    
    return db_user

def login_user(db: Session, login_data: UserLogin) -> Token:
    """Login user and return tokens."""
    user = authenticate_user(db, login_data.username_or_email, login_data.password)
    
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username/email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Inactive user account"
        )
    
    # Update last login
    user.last_login = datetime.now(timezone.utc)
    db.commit()
    
    # Create tokens
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.id, "username": user.username},
        expires_delta=access_token_expires
    )
    
    refresh_token = create_refresh_token(
        data={"sub": user.id, "username": user.username}
    )
    
    return Token(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )

def refresh_access_token(db: Session, refresh_token: str) -> Token:
    """Refresh access token using refresh token."""
    token_data = verify_token(refresh_token, "refresh")
    
    if token_data is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid refresh token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    user = db.query(User).filter(User.id == token_data.user_id).first()
    
    if user is None or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or inactive",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Create new access token
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.id, "username": user.username},
        expires_delta=access_token_expires
    )
    
    return Token(
        access_token=access_token,
        refresh_token=refresh_token,  # Keep the same refresh token
        expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )

# Dependency for getting current user

async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    """Get current authenticated user."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    
    if credentials is None:
        raise credentials_exception
    
    token_data = verify_token(credentials.credentials)
    
    if token_data is None:
        raise credentials_exception
    
    user = db.query(User).filter(User.id == token_data.user_id).first()
    
    if user is None:
        raise credentials_exception
    
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Inactive user"
        )
    
    return user

async def get_current_active_user(current_user: User = Depends(get_current_user)) -> User:
    """Get current active user (alias for better semantics)."""
    return current_user

# Optional authentication (for endpoints that can work with or without auth)
async def get_current_user_optional(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
    db: Session = Depends(get_db)
) -> Optional[User]:
    """Get current user if authenticated, None otherwise."""
    if credentials is None:
        return None
    
    try:
        token_data = verify_token(credentials.credentials)
        if token_data is None:
            return None
        
        user = db.query(User).filter(User.id == token_data.user_id).first()
        if user and user.is_active:
            return user
        return None
    except:
        return None

# Profile management functions

def update_user_profile(db: Session, user: User, profile_data: UserProfileUpdate) -> User:
    """Update user profile."""
    for field, value in profile_data.dict(exclude_unset=True).items():
        setattr(user, field, value)
    
    user.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(user)
    
    return user

def get_user_preferences(db: Session, user_id: str) -> Optional[UserPreferences]:
    """Get user preferences."""
    return db.query(UserPreferences).filter(UserPreferences.user_id == user_id).first()

def update_user_preferences(
    db: Session, 
    user_id: str, 
    preferences_data: UserPreferencesModel
) -> UserPreferences:
    """Update user preferences."""
    preferences = get_user_preferences(db, user_id)
    
    if not preferences:
        # Create new preferences if they don't exist
        preferences = UserPreferences(user_id=user_id)
        db.add(preferences)
    
    for field, value in preferences_data.dict(exclude_unset=True).items():
        setattr(preferences, field, value)
    
    preferences.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(preferences)
    
    return preferences

def deactivate_user(db: Session, user: User) -> User:
    """Deactivate user account."""
    user.is_active = False
    user.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(user)
    
    return user

def change_password(db: Session, user: User, old_password: str, new_password: str) -> bool:
    """Change user password."""
    if not verify_password(old_password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Incorrect current password"
        )
    
    user.hashed_password = get_password_hash(new_password)
    user.updated_at = datetime.now(timezone.utc)
    db.commit()
    
    return True

# Utility functions

def is_email_available(db: Session, email: str) -> bool:
    """Check if email is available."""
    return get_user_by_email(db, email) is None

def is_username_available(db: Session, username: str) -> bool:
    """Check if username is available."""
    return get_user_by_username(db, username) is None

# Security middleware

class SecurityMiddleware:
    """Custom security middleware for additional checks."""
    
    @staticmethod
    def validate_request_origin(request: Request) -> bool:
        """Validate request origin (implement CORS logic here)."""
        # Implement your CORS validation logic
        return True
    
    @staticmethod
    def rate_limit_check(request: Request, user_id: str) -> bool:
        """Check rate limits for user requests."""
        # Implement rate limiting logic here
        return True

if __name__ == "__main__":
    print("User management module for SwingSync AI loaded.")
    print("Available functions:")
    print("- User registration and authentication")
    print("- JWT token management")
    print("- Profile management")
    print("- Password utilities")