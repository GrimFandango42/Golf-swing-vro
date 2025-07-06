# SwingSync AI - User Profile Management & Database Integration

## Overview

This implementation adds comprehensive user profile management and database integration to the SwingSync AI golf swing analysis system. The system now supports user authentication, data persistence, and historical tracking of swing analysis results.

## Architecture

### Database Layer (`database.py`)
- **SQLAlchemy ORM** for database operations
- **Multiple database support** (SQLite for development, PostgreSQL/MySQL for production)
- **Comprehensive models** for users, sessions, and analysis data
- **Relationship management** between entities
- **Database utilities** and helper functions

### Authentication Layer (`user_management.py`)
- **JWT token-based authentication** with access and refresh tokens
- **Password hashing** using bcrypt
- **User registration and login** with validation
- **Profile management** with comprehensive user data
- **Security middleware** and rate limiting support

### API Layer (`main.py`)
- **Protected endpoints** requiring authentication
- **User management endpoints** for registration, login, profile updates
- **Enhanced swing analysis** with data persistence
- **User analytics** and session history
- **CORS support** and comprehensive error handling

## Key Features

### 1. User Authentication
- Secure registration with email/username validation
- JWT-based authentication with refresh tokens
- Password strength requirements and secure hashing
- Account activation and verification support

### 2. User Profiles
- Comprehensive user information (skill level, handicap, physical stats)
- Customizable preferences (units, feedback detail, focus areas)
- Privacy controls and data sharing preferences
- Goal setting and progress tracking

### 3. Data Persistence
- Automatic saving of all swing analysis results
- Historical tracking of KPIs and fault detection
- Session metadata and video information storage
- Detailed analysis results with confidence scores

### 4. User Analytics
- Personal performance statistics and trends
- Common fault identification and tracking
- Progress monitoring over time
- Session history with filtering and pagination

## Database Schema

### Users Table
```sql
- id (Primary Key)
- email (Unique)
- username (Unique)
- hashed_password
- first_name, last_name
- skill_level (enum)
- handicap, preferred_hand
- height_cm, weight_kg
- account status fields
- timestamps
```

### User Preferences Table
```sql
- user_id (Foreign Key)
- preferred_units (metric/imperial)
- feedback_detail_level
- focus_areas (JSON array)
- notification preferences
- goals and targets
- privacy settings
```

### Swing Sessions Table
```sql
- id (session_id from input)
- user_id (Foreign Key)
- club_used
- session_status (enum)
- video metadata
- P-System classification data
- processing information
- timestamps
```

### Analysis Results Table
```sql
- session_id (Foreign Key)
- summary_of_findings
- overall_score
- detailed_feedback (JSON)
- raw_detected_faults (JSON)
- confidence metrics
```

### Biomechanical KPIs Table
```sql
- session_id (Foreign Key)
- p_position
- kpi_name
- value, unit
- ideal ranges
- deviation calculations
```

### Detected Faults Table
```sql
- session_id (Foreign Key)
- fault identification
- severity levels
- P-position context
- corrective feedback
- drill suggestions
```

## API Endpoints

### Authentication Endpoints
- `POST /auth/register` - Register new user
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh access token
- `GET /auth/me` - Get current user profile
- `PUT /auth/profile` - Update user profile
- `POST /auth/change-password` - Change password
- `GET /auth/preferences` - Get user preferences
- `PUT /auth/preferences` - Update preferences

### Utility Endpoints
- `GET /auth/check-email/{email}` - Check email availability
- `GET /auth/check-username/{username}` - Check username availability

### Swing Analysis Endpoints
- `POST /analyze_swing/` - Analyze swing (now requires authentication)

### User Data Endpoints
- `GET /user/sessions` - Get session history with pagination
- `GET /user/session/{session_id}` - Get detailed session information
- `GET /user/analytics` - Get user analytics and statistics

### Health Endpoints
- `GET /` - API information and feature list
- `GET /health` - Health check for monitoring

## Security Features

### JWT Authentication
- Access tokens (30-minute expiry by default)
- Refresh tokens (7-day expiry by default)
- Secure token signing with configurable secret key
- Automatic token validation on protected endpoints

### Password Security
- Bcrypt hashing with salt
- Minimum password requirements
- Secure password change process
- Account lockout protection (configurable)

### Data Protection
- User data isolation (users can only access their own data)
- Privacy controls for data sharing
- Secure session management
- CORS configuration for cross-origin requests

## Configuration

### Environment Variables
```bash
# Required
SECRET_KEY=your_jwt_signing_key
GEMINI_API_KEY=your_gemini_api_key

# Optional
DATABASE_URL=sqlite:///./swingsync.db
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7
```

### Database Configuration
- SQLite for development (default)
- PostgreSQL for production (recommended)
- MySQL support available
- Connection pooling and session management

## Setup Instructions

### 1. Quick Setup
```bash
# Run the automated setup script
python setup.py
```

### 2. Manual Setup
```bash
# Install dependencies
pip install -r requirements.txt

# Set up environment
cp .env.example .env
# Edit .env with your configuration

# Initialize database
python migrate.py init

# (Optional) Add sample data
python migrate.py seed

# Start the server
uvicorn main:app --reload
```

### 3. Database Management
```bash
# Check database status
python migrate.py status

# Create backup
python migrate.py backup

# Reset database (WARNING: deletes all data)
python migrate.py reset

# Upgrade schema
python migrate.py upgrade
```

## Usage Examples

### 1. User Registration
```python
import requests

response = requests.post("http://localhost:8000/auth/register", json={
    "email": "golfer@example.com",
    "username": "pro_golfer",
    "password": "secure_password123",
    "first_name": "Tiger",
    "last_name": "Woods",
    "skill_level": "professional",
    "handicap": 0.0,
    "preferred_hand": "right"
})
```

### 2. User Login
```python
response = requests.post("http://localhost:8000/auth/login", json={
    "username_or_email": "golfer@example.com",
    "password": "secure_password123"
})
tokens = response.json()
access_token = tokens["access_token"]
```

### 3. Analyze Swing (Authenticated)
```python
headers = {"Authorization": f"Bearer {access_token}"}
response = requests.post("http://localhost:8000/analyze_swing/", 
                        json=swing_data, headers=headers)
```

### 4. Get User Analytics
```python
headers = {"Authorization": f"Bearer {access_token}"}
response = requests.get("http://localhost:8000/user/analytics", headers=headers)
analytics = response.json()
```

## Migration and Deployment

### Database Migrations
- Use `migrate.py` for schema management
- Supports backup and restore operations
- Version tracking for schema changes
- Safe upgrade and rollback procedures

### Production Deployment
1. Use PostgreSQL or MySQL for production
2. Set strong JWT secret keys
3. Configure CORS origins appropriately
4. Set up database backups
5. Monitor with health check endpoints
6. Use environment-specific configurations

## Performance Considerations

### Database Optimization
- Indexed foreign keys and query columns
- Efficient relationship loading
- Pagination for large datasets
- Connection pooling

### Caching Strategies
- JWT token validation caching
- User session caching
- Analysis result caching for repeated requests
- Database query result caching

### Scalability
- Stateless authentication (JWT)
- Database connection pooling
- Horizontal scaling support
- Microservice-ready architecture

## Monitoring and Maintenance

### Health Checks
- Database connectivity monitoring
- API endpoint health checks
- Performance metrics tracking
- Error rate monitoring

### Logging
- Authentication events
- API request/response logging
- Database operation logging
- Error and exception tracking

### Backup Strategy
- Automated database backups
- User data export capabilities
- Disaster recovery procedures
- Data retention policies

## Security Best Practices

### Authentication
- Strong password requirements
- JWT token expiration management
- Secure token storage recommendations
- Account lockout after failed attempts

### Authorization
- Role-based access control ready
- User data isolation
- API rate limiting
- Input validation and sanitization

### Data Protection
- Encrypted password storage
- Secure API communication (HTTPS)
- Privacy controls implementation
- GDPR compliance considerations

## Development Guidelines

### Code Organization
- Modular architecture with clear separation
- Database models in dedicated module
- Authentication logic isolated
- Comprehensive error handling

### Testing Strategy
- Unit tests for authentication functions
- Integration tests for API endpoints
- Database operation testing
- Performance testing recommendations

### Documentation
- Comprehensive API documentation (Swagger/OpenAPI)
- Database schema documentation
- Setup and deployment guides
- User guide and examples

## Troubleshooting

### Common Issues
1. **Database Connection Errors**
   - Check DATABASE_URL configuration
   - Verify database server is running
   - Check user permissions

2. **Authentication Failures** 
   - Verify SECRET_KEY is set
   - Check token expiration settings
   - Validate user credentials

3. **Migration Issues**
   - Use `migrate.py status` to check state
   - Backup before running migrations
   - Check for conflicting schema changes

### Support
- Check logs for detailed error messages
- Use health check endpoints for diagnostics
- Review environment configuration
- Consult API documentation at `/docs`

This implementation provides a robust, scalable foundation for user management and data persistence in the SwingSync AI system, enabling comprehensive tracking of user progress and swing analysis history.