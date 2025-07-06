#!/usr/bin/env python3
"""
Database migration script for SwingSync AI.

This script provides utilities for managing database schema changes,
including creation, migration, and data management operations.

Usage:
    python migrate.py init          # Initialize new database
    python migrate.py upgrade       # Upgrade to latest schema  
    python migrate.py reset         # Reset database (WARNING: deletes all data)
    python migrate.py seed          # Add sample/test data
    python migrate.py backup       # Backup database
    python migrate.py status        # Show current database status
"""

import sys
import os
import argparse
from datetime import datetime
import json
import uuid

# Add current directory to path for imports
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from database import (
    engine, Base, SessionLocal, User, UserPreferences, SwingSession,
    SwingAnalysisResult, BiomechanicalKPI, DetectedFault,
    SessionStatus, FaultSeverity, SkillLevel,
    create_tables, drop_tables, init_database
)
from user_management import get_password_hash

def init_db():
    """Initialize a new database with all tables."""
    print("Initializing database...")
    try:
        create_tables()
        print("✅ Database tables created successfully!")
        return True
    except Exception as e:
        print(f"❌ Error initializing database: {e}")
        return False

def reset_db():
    """Reset database by dropping and recreating all tables."""
    response = input("⚠️  This will DELETE ALL DATA. Type 'yes' to continue: ")
    if response.lower() != 'yes':
        print("Operation cancelled.")
        return False
    
    print("Resetting database...")
    try:
        drop_tables()
        create_tables()
        print("✅ Database reset successfully!")
        return True
    except Exception as e:
        print(f"❌ Error resetting database: {e}")
        return False

def upgrade_db():
    """Upgrade database schema to latest version."""
    print("Upgrading database schema...")
    try:
        # In a production system, this would use Alembic migrations
        # For now, we'll just ensure all tables exist
        Base.metadata.create_all(bind=engine)
        print("✅ Database schema upgraded successfully!")
        return True
    except Exception as e:
        print(f"❌ Error upgrading database: {e}")
        return False

def seed_db():
    """Add sample data to the database for testing."""
    print("Seeding database with sample data...")
    
    db = SessionLocal()
    try:
        # Check if data already exists
        existing_users = db.query(User).count()
        if existing_users > 0:
            response = input(f"Database already has {existing_users} users. Continue? (y/n): ")
            if response.lower() != 'y':
                print("Seeding cancelled.")
                return False
        
        # Create sample users
        sample_users = [
            {
                "id": str(uuid.uuid4()),
                "email": "john.doe@example.com",
                "username": "john_doe",
                "password": "password123",
                "first_name": "John",
                "last_name": "Doe",
                "skill_level": SkillLevel.INTERMEDIATE,
                "handicap": 12.5,
                "preferred_hand": "right",
                "height_cm": 180.0,
                "weight_kg": 75.0
            },
            {
                "id": str(uuid.uuid4()),
                "email": "jane.smith@example.com", 
                "username": "jane_smith",
                "password": "password123",
                "first_name": "Jane",
                "last_name": "Smith",
                "skill_level": SkillLevel.ADVANCED,
                "handicap": 8.2,
                "preferred_hand": "right",
                "height_cm": 165.0,
                "weight_kg": 60.0
            },
            {
                "id": str(uuid.uuid4()),
                "email": "beginner@example.com",
                "username": "golf_newbie",
                "password": "password123",
                "first_name": "Alex",
                "last_name": "Beginner",
                "skill_level": SkillLevel.BEGINNER,
                "handicap": 28.0,
                "preferred_hand": "left",
                "height_cm": 175.0,
                "weight_kg": 70.0
            }
        ]
        
        created_users = []
        for user_data in sample_users:
            # Create user
            user = User(
                id=user_data["id"],
                email=user_data["email"],
                username=user_data["username"],
                hashed_password=get_password_hash(user_data["password"]),
                first_name=user_data["first_name"],
                last_name=user_data["last_name"],
                skill_level=user_data["skill_level"],
                handicap=user_data["handicap"],
                preferred_hand=user_data["preferred_hand"],
                height_cm=user_data["height_cm"],
                weight_kg=user_data["weight_kg"],
                is_active=True,
                is_verified=True
            )
            db.add(user)
            created_users.append(user)
            
            # Create user preferences
            preferences = UserPreferences(
                user_id=user.id,
                preferred_units="metric",
                feedback_detail_level="detailed",
                focus_areas=["swing_plane", "tempo", "balance"],
                email_notifications=True,
                push_notifications=True,
                weekly_reports=True,
                target_handicap=user_data["handicap"] - 2.0,
                primary_goals=["improve_consistency", "increase_distance"]
            )
            db.add(preferences)
        
        db.commit()
        
        # Create sample swing sessions for the first user
        first_user = created_users[0]
        sample_sessions = []
        
        for i in range(3):
            session_id = str(uuid.uuid4())
            session = SwingSession(
                id=session_id,
                user_id=first_user.id,
                club_used=["Driver", "7-Iron", "Pitching Wedge"][i],
                session_status=SessionStatus.COMPLETED,
                video_fps=240.0,
                total_frames=120,
                video_duration_seconds=0.5,
                processing_time_seconds=2.3,
                p_system_phases=[
                    {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 15},
                    {"phase_name": "P2", "start_frame_index": 16, "end_frame_index": 30},
                    {"phase_name": "P3", "start_frame_index": 31, "end_frame_index": 45},
                    {"phase_name": "P4", "start_frame_index": 46, "end_frame_index": 60},
                    {"phase_name": "P5", "start_frame_index": 61, "end_frame_index": 75},
                    {"phase_name": "P6", "start_frame_index": 76, "end_frame_index": 90},
                    {"phase_name": "P7", "start_frame_index": 91, "end_frame_index": 105},
                    {"phase_name": "P8", "start_frame_index": 106, "end_frame_index": 120}
                ],
                completed_at=datetime.utcnow()
            )
            db.add(session)
            sample_sessions.append(session)
            
            # Create analysis result
            analysis = SwingAnalysisResult(
                session_id=session_id,
                summary_of_findings=f"Sample analysis for {session.club_used} swing. Overall solid technique with some areas for improvement.",
                overall_score=75.0 + (i * 5),  # Scores of 75, 80, 85
                detailed_feedback=[
                    {
                        "explanation": "Your backswing shows good shoulder rotation but could benefit from more hip engagement.",
                        "tip": "Focus on initiating the backswing with a slight hip turn to create better coil.",
                        "drill_suggestion": "Practice the 'chair drill' - place a chair behind you and try to touch it with your trailing hip during backswing."
                    }
                ],
                raw_detected_faults=[],
                confidence_score=0.92
            )
            db.add(analysis)
            
            # Create sample KPIs
            sample_kpis = [
                {
                    "p_position": "P4",
                    "kpi_name": "Shoulder Rotation",
                    "value": 95.0 + (i * 2),
                    "unit": "degrees",
                    "ideal_min": 90.0,
                    "ideal_max": 110.0
                },
                {
                    "p_position": "P6", 
                    "kpi_name": "Hip-Shoulder Separation",
                    "value": 45.0 - (i * 2),
                    "unit": "degrees",
                    "ideal_min": 35.0,
                    "ideal_max": 50.0
                }
            ]
            
            for kpi_data in sample_kpis:
                kpi = BiomechanicalKPI(
                    session_id=session_id,
                    p_position=kpi_data["p_position"],
                    kpi_name=kpi_data["kpi_name"],
                    value=kpi_data["value"],
                    unit=kpi_data["unit"],
                    ideal_min=kpi_data["ideal_min"],
                    ideal_max=kpi_data["ideal_max"],
                    deviation_from_ideal=0.0,
                    confidence=0.88
                )
                db.add(kpi)
        
        db.commit()
        
        print(f"✅ Sample data created successfully!")
        print(f"   - {len(sample_users)} users created")
        print(f"   - {len(sample_sessions)} swing sessions created")
        print(f"   - Sample login: john.doe@example.com / password123")
        
        return True
        
    except Exception as e:
        db.rollback()
        print(f"❌ Error seeding database: {e}")
        return False
    finally:
        db.close()

def backup_db():
    """Create a backup of the database."""
    print("Creating database backup...")
    
    # For SQLite databases
    if "sqlite" in str(engine.url):
        import shutil
        db_path = str(engine.url).replace("sqlite:///", "")
        backup_path = f"{db_path}.backup.{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        try:
            shutil.copy2(db_path, backup_path)
            print(f"✅ Database backed up to: {backup_path}")
            return True
        except Exception as e:
            print(f"❌ Error creating backup: {e}")
            return False
    else:
        print("⚠️  Backup not implemented for this database type")
        print("   Use your database-specific backup tools")
        return False

def show_status():
    """Show current database status."""
    print("Database Status")
    print("=" * 50)
    
    try:
        # Check if tables exist
        Base.metadata.reflect(bind=engine)
        tables = Base.metadata.tables.keys()
        print(f"Database URL: {engine.url}")
        print(f"Tables found: {len(tables)}")
        
        if tables:
            print("Tables:")
            for table in sorted(tables):
                print(f"  - {table}")
        
        # Check data counts
        db = SessionLocal()
        try:
            user_count = db.query(User).count()
            session_count = db.query(SwingSession).count()
            analysis_count = db.query(SwingAnalysisResult).count()
            
            print(f"\nData Summary:")
            print(f"  - Users: {user_count}")
            print(f"  - Swing Sessions: {session_count}")
            print(f"  - Analysis Results: {analysis_count}")
            
        except Exception as e:
            print(f"⚠️  Could not query data counts: {e}")
        finally:
            db.close()
            
    except Exception as e:
        print(f"❌ Error checking database status: {e}")
        return False
    
    return True

def main():
    """Main CLI interface."""
    parser = argparse.ArgumentParser(description="SwingSync AI Database Migration Tool")
    parser.add_argument(
        "command",
        choices=["init", "upgrade", "reset", "seed", "backup", "status"],
        help="Migration command to execute"
    )
    
    args = parser.parse_args()
    
    print("SwingSync AI Database Migration Tool")
    print("=" * 40)
    
    success = False
    
    if args.command == "init":
        success = init_db()
    elif args.command == "upgrade":
        success = upgrade_db()
    elif args.command == "reset":
        success = reset_db()
    elif args.command == "seed":
        success = seed_db()
    elif args.command == "backup":
        success = backup_db()
    elif args.command == "status":
        success = show_status()
    
    if success:
        print("\n🎉 Operation completed successfully!")
    else:
        print("\n💥 Operation failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()