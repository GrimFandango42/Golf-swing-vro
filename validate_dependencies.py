#!/usr/bin/env python3
"""
Dependency Validation Script
Validates all Gradle dependencies before pushing to GitHub Actions
"""

import requests
import re
import sys
from urllib.parse import urljoin

# Repository URLs to check
REPOSITORIES = [
    "https://repo1.maven.org/maven2/",  # Maven Central
    "https://dl.google.com/dl/android/maven2/",  # Google
    "https://jitpack.io/",  # JitPack
    "https://storage.googleapis.com/download.tensorflow.org/maven/",  # TensorFlow
]

def check_dependency(group_id, artifact_id, version, repositories):
    """Check if a dependency exists in any of the repositories"""
    print(f"Checking {group_id}:{artifact_id}:{version}")
    
    # Convert to path format
    group_path = group_id.replace('.', '/')
    artifact_path = f"{group_path}/{artifact_id}/{version}/{artifact_id}-{version}.pom"
    
    for repo_url in repositories:
        full_url = urljoin(repo_url, artifact_path)
        try:
            response = requests.head(full_url, timeout=10)
            if response.status_code == 200:
                print(f"  ✅ Found at: {repo_url}")
                return True
            else:
                print(f"  ❌ Not found at {repo_url} (status: {response.status_code})")
        except Exception as e:
            print(f"  ⚠️ Error checking {repo_url}: {e}")
    
    return False

def parse_build_gradle():
    """Parse build.gradle to extract dependencies"""
    dependencies = []
    
    try:
        with open('app/build.gradle', 'r') as f:
            content = f.read()
        
        # Find implementation lines
        impl_pattern = r'implementation\s+"([^"]+)"'
        matches = re.findall(impl_pattern, content)
        
        for match in matches:
            if ':' in match:
                parts = match.split(':')
                if len(parts) >= 3:
                    group_id = parts[0]
                    artifact_id = parts[1]
                    version = parts[2]
                    dependencies.append((group_id, artifact_id, version))
    
    except Exception as e:
        print(f"Error parsing build.gradle: {e}")
    
    return dependencies

def main():
    print("🔍 Validating Gradle Dependencies")
    print("=" * 50)
    
    dependencies = parse_build_gradle()
    
    if not dependencies:
        print("❌ No dependencies found to validate")
        sys.exit(1)
    
    failed_deps = []
    
    for group_id, artifact_id, version in dependencies:
        if not check_dependency(group_id, artifact_id, version, REPOSITORIES):
            failed_deps.append(f"{group_id}:{artifact_id}:{version}")
        print()
    
    print("=" * 50)
    if failed_deps:
        print("❌ FAILED DEPENDENCIES:")
        for dep in failed_deps:
            print(f"  • {dep}")
        print(f"\nTotal failed: {len(failed_deps)}")
        sys.exit(1)
    else:
        print("✅ ALL DEPENDENCIES VALIDATED SUCCESSFULLY!")

if __name__ == "__main__":
    main()