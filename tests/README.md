# SwingSync AI Comprehensive Test Suite

This directory contains a comprehensive test suite for the SwingSync AI golf swing analysis system. The test suite covers all major components and provides validation for performance, functionality, and reliability requirements.

## Test Structure

### Core Test Files

#### 1. **test_integration.py** - Complete Pipeline Integration Tests
- End-to-end analysis pipeline testing
- Club-specific analysis integration
- Database persistence integration
- Streaming analysis integration
- Error handling and recovery scenarios
- Multi-user session management

#### 2. **test_performance.py** - Performance Benchmarks and Validation
- **Latency Testing**: Sub-100ms real-time analysis requirements
- **Throughput Testing**: Batch processing capabilities
- **Memory Usage**: <500MB limit validation
- **Scalability**: Concurrent user testing (50+ simultaneous)
- **Database Performance**: <50ms query performance
- **Regression Detection**: Performance baseline monitoring

#### 3. **test_streaming.py** - Real-time Streaming Endpoint Tests
- WebSocket connection management
- Real-time frame processing
- Live coaching session functionality
- Performance monitoring endpoints
- Concurrent streaming sessions
- Message broadcasting and routing

#### 4. **test_database.py** - Database and User Management Tests
- Database model relationships
- User authentication and authorization
- Data integrity and validation
- Query performance optimization
- Transaction management
- Concurrent access testing

#### 5. **Enhanced Existing Tests**
- **test_kpi_extraction.py**: Enhanced P-position KPI testing
- **test_fault_detection.py**: Club-specific fault detection
- **test_feedback_generation.py**: Enhanced feedback testing

### Supporting Modules

#### **mock_data_factory.py** - Enhanced Mock Data Generator
- Physics-based swing trajectory generation
- Club-specific swing variations (Driver, Irons, Wedges)
- Realistic fault injection for testing
- Streaming data simulation
- Database entity factories
- Performance test datasets

#### **mock_gemini_api.py** - Mock Gemini API for Testing
- Realistic feedback generation without API costs
- Configurable response patterns
- Error simulation for robust testing
- Performance simulation with delays
- Context-aware responses based on detected faults
- Streaming response simulation

#### **conftest.py** - Pytest Configuration and Fixtures
- Database setup and cleanup
- Mock API services
- Performance monitoring utilities
- Parameterized test scenarios
- Automatic test environment setup

## Key Features

### 🎯 **Performance Requirements Validation**
- **Real-time Analysis**: < 100ms per frame
- **Batch Processing**: < 2s per complete swing
- **Memory Usage**: < 500MB for typical workloads
- **Concurrent Users**: 50+ simultaneous analyses
- **Database Queries**: < 50ms for typical operations

### 🏌️ **Golf-Specific Testing**
- **All P-Positions**: P1-P10 comprehensive testing
- **Club-Specific Rules**: Driver, Iron, Wedge variations
- **Fault Detection**: 20+ biomechanical fault scenarios
- **Real-time Analysis**: Frame-by-frame validation
- **Streaming Integration**: Live coaching scenarios

### 🔧 **Technical Coverage**
- **API Integration**: Mock Gemini responses
- **Database Operations**: Full CRUD testing
- **WebSocket Communication**: Real-time streaming
- **Error Handling**: Edge cases and recovery
- **Security**: Authentication and authorization

## Running Tests

### Quick Start
```bash
# Install dependencies
pip install -r requirements.txt
pip install pytest pytest-asyncio pytest-cov

# Run all tests
python run_tests.py

# Run specific test categories
python run_tests.py --unit          # Unit tests only
python run_tests.py --integration   # Integration tests
python run_tests.py --performance   # Performance tests
python run_tests.py --streaming     # Streaming tests
python run_tests.py --database      # Database tests
```

### Test Options
```bash
# Fast tests (skip slow performance tests)
python run_tests.py --fast

# Full performance benchmarks
python run_tests.py --benchmark

# Generate coverage report
python run_tests.py --coverage

# Pytest directly with markers
pytest -m "not slow"              # Skip slow tests
pytest -m "performance"           # Performance tests only
pytest -m "integration"           # Integration tests only
pytest -v tests/test_streaming.py # Verbose streaming tests
```

### Performance Testing
```bash
# Quick performance check
pytest tests/test_performance.py -m "not slow" -v

# Full performance suite (includes load testing)
pytest tests/test_performance.py -v

# Specific performance categories
pytest tests/test_performance.py::TestLatencyBenchmarks -v
pytest tests/test_performance.py::TestMemoryUsage -v
```

## Test Categories and Markers

### Pytest Markers
- `@pytest.mark.slow`: Tests that take >1 second
- `@pytest.mark.integration`: Integration tests
- `@pytest.mark.performance`: Performance benchmarks
- `@pytest.mark.streaming`: WebSocket/streaming tests
- `@pytest.mark.database`: Database operation tests

### Test Selection Examples
```bash
# Run only fast tests
pytest -m "not slow"

# Run performance tests only
pytest -m "performance"

# Run streaming and database tests
pytest -m "streaming or database"

# Skip integration tests
pytest -m "not integration"
```

## Mock Data and APIs

### Mock Data Factory
The `mock_data_factory.py` provides realistic test data:

```python
from tests.mock_data_factory import create_realistic_swing, ClubType, SwingQuality

# Create realistic driver swing
swing_data = create_realistic_swing(
    club_type=ClubType.DRIVER,
    quality=SwingQuality.GOOD
)

# Create faulty swing for testing
poor_swing = create_realistic_swing(
    quality=SwingQuality.POOR,
    specific_faults=["insufficient_shoulder_turn", "cupped_wrist"]
)
```

### Mock Gemini API
The `mock_gemini_api.py` eliminates external API dependencies:

```python
from tests.mock_gemini_api import get_mock_gemini_api, MockGeminiConfig

# Configure mock API
config = MockGeminiConfig(
    response_quality=MockResponseQuality.GOOD,
    response_mode=MockResponseMode.FAST
)

mock_api = get_mock_gemini_api(config)
```

## Performance Benchmarks

### Expected Performance Metrics

| Component | Requirement | Test Validation |
|-----------|-------------|-----------------|
| Frame Analysis | <100ms | ✅ Real-time latency tests |
| Complete Swing | <2s | ✅ End-to-end pipeline tests |
| Database Queries | <50ms | ✅ Query performance tests |
| Memory Usage | <500MB | ✅ Memory monitoring tests |
| Concurrent Users | 50+ users | ✅ Load testing scenarios |

### Performance Test Results
Run performance tests to see current metrics:
```bash
python run_tests.py --performance --benchmark
```

## Database Testing

### Test Database Setup
- **SQLite In-Memory**: Fast, isolated testing
- **Automatic Cleanup**: No test data persistence
- **Transaction Testing**: Rollback and isolation
- **Performance Validation**: Query optimization

### Database Test Coverage
- User authentication and management
- Swing session persistence
- Analysis result storage
- KPI and fault data relationships
- Query performance optimization
- Concurrent access scenarios

## Streaming Tests

### WebSocket Testing
- Connection lifecycle management
- Real-time message handling
- Session management
- Error handling and recovery
- Performance under load

### Live Coaching Tests
- Multi-participant sessions
- Message broadcasting
- Real-time feedback delivery
- Session synchronization

## Integration Test Scenarios

### Complete Pipeline Testing
1. **Input Processing**: Pose data validation
2. **KPI Extraction**: All P-position calculations
3. **Fault Detection**: Club-specific rule application
4. **Feedback Generation**: AI-powered insights
5. **Database Persistence**: Result storage
6. **API Response**: Client delivery

### Error Handling Scenarios
- Invalid input data
- Missing keypoints
- Database connection failures
- API timeouts
- Memory constraints

## Contributing to Tests

### Adding New Tests
1. **Follow Naming Convention**: `test_feature_name.py`
2. **Use Fixtures**: Leverage `conftest.py` fixtures
3. **Add Performance Tests**: For new algorithms
4. **Mock External Dependencies**: No real API calls
5. **Document Test Purpose**: Clear docstrings

### Test Best Practices
- **Isolated Tests**: No dependencies between tests
- **Realistic Data**: Use mock data factory
- **Performance Aware**: Mark slow tests appropriately
- **Error Scenarios**: Test edge cases and failures
- **Documentation**: Clear test descriptions

## Continuous Integration

### Test Automation
The test suite is designed for CI/CD integration:

```yaml
# Example GitHub Actions configuration
- name: Run SwingSync AI Tests
  run: |
    pip install -r requirements.txt
    python run_tests.py --coverage
    
- name: Performance Validation
  run: |
    python run_tests.py --performance --fast
```

### Quality Gates
- **Test Coverage**: >85% code coverage
- **Performance**: All benchmarks must pass
- **Integration**: End-to-end scenarios validated
- **No Regressions**: Performance baseline maintained

## Troubleshooting

### Common Issues

#### Import Errors
```bash
# Add project root to Python path
export PYTHONPATH="${PYTHONPATH}:$(pwd)"
```

#### Database Connection Issues
```bash
# Tests use in-memory SQLite - no external database needed
pytest tests/test_database.py -v
```

#### Performance Test Failures
```bash
# Run on dedicated hardware for consistent results
# Adjust thresholds in test files if needed
pytest tests/test_performance.py::TestLatencyBenchmarks -v
```

#### WebSocket Test Issues
```bash
# Ensure no conflicting services on test ports
pytest tests/test_streaming.py -v -s
```

### Debug Mode
```bash
# Verbose output with print statements
pytest -v -s tests/test_integration.py

# Debug specific test
pytest --pdb tests/test_fault_detection.py::TestClubSpecificFaultDetection::test_club_type_classification
```

## Test Reports

### Coverage Reports
```bash
# Generate HTML coverage report
python run_tests.py --coverage
# View: htmlcov/index.html
```

### Performance Reports
Performance metrics are printed during test execution and can be captured for analysis.

## Architecture Testing

The test suite validates the entire SwingSync AI architecture:

```
📱 Mobile App Input
    ↓
🔍 Pose Detection → KPI Extraction → Fault Detection
    ↓                    ↓              ↓
💾 Database ← 🤖 AI Feedback ← 📊 Analysis Engine
    ↓
📡 Real-time Streaming ← 👨‍🏫 Live Coaching
    ↓
📱 Client Response
```

Each component is thoroughly tested with realistic data and performance validation.

---

## Summary

This comprehensive test suite ensures SwingSync AI meets all performance, functionality, and reliability requirements. The combination of unit tests, integration tests, performance benchmarks, and realistic mock data provides confidence in the system's robustness and readiness for production deployment.

For questions or contributions, refer to the main project documentation.