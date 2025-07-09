#!/usr/bin/env python3
"""
SwingSync AI Mobile Demo
========================
A lightweight demo that runs on Termux without heavy dependencies
"""

import json
import os
from datetime import datetime
import socket

# Simple web server
try:
    from http.server import HTTPServer, SimpleHTTPRequestHandler
    import urllib.parse
except ImportError:
    print("Basic HTTP server not available")

class SwingSyncMobileDemo:
    def __init__(self):
        self.version = "1.0.0"
        self.features = [
            "AI Swing Analysis with 32 KPIs",
            "X-Factor Power Generation Analysis", 
            "Conversational Voice Coaching",
            "Real-time Performance Feedback",
            "Progress Tracking & Analytics",
            "Celebration & Achievement System"
        ]
    
    def get_device_info(self):
        """Get basic device information"""
        try:
            import platform
            return {
                "platform": platform.system(),
                "machine": platform.machine(),
                "python_version": platform.python_version(),
                "hostname": socket.gethostname()
            }
        except:
            return {"platform": "Android/Termux", "status": "running"}
    
    def get_local_ip(self):
        """Get local IP address"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return "localhost"
    
    def demo_swing_analysis(self):
        """Demo swing analysis without dependencies"""
        return {
            "session_id": "demo_session_001",
            "analysis_results": {
                "overall_score": 87.5,
                "x_factor": 45.2,
                "tempo_score": 92.1,
                "balance_score": 84.6,
                "power_rating": "Excellent",
                "key_insights": [
                    "Great X-Factor separation for power generation",
                    "Excellent tempo consistency", 
                    "Minor adjustment needed in follow-through balance"
                ]
            },
            "coaching_feedback": "Outstanding swing! Your X-Factor of 45.2° is in the optimal range for power generation. Keep working on that smooth tempo - it's really paying off!",
            "timestamp": datetime.now().isoformat()
        }
    
    def generate_mobile_ui(self):
        """Generate mobile-friendly HTML interface"""
        device_info = self.get_device_info()
        local_ip = self.get_local_ip()
        demo_analysis = self.demo_swing_analysis()
        
        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SwingSync AI Mobile Demo</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #2E7D32, #1976D2);
            color: white;
            min-height: 100vh;
            padding: 20px;
        }}
        .container {{
            max-width: 800px;
            margin: 0 auto;
            background: rgba(255,255,255,0.1);
            border-radius: 20px;
            padding: 30px;
            backdrop-filter: blur(10px);
        }}
        h1 {{ text-align: center; font-size: 2.5em; margin-bottom: 10px; }}
        .subtitle {{ text-align: center; font-size: 1.2em; opacity: 0.9; margin-bottom: 30px; }}
        .status {{
            background: rgba(76, 175, 80, 0.3);
            border-radius: 15px;
            padding: 20px;
            margin: 20px 0;
            border-left: 4px solid #4CAF50;
        }}
        .feature {{
            background: rgba(255,255,255,0.1);
            border-radius: 15px;
            padding: 20px;
            margin: 15px 0;
            border-left: 4px solid #FFB300;
        }}
        .demo-result {{
            background: rgba(255, 193, 7, 0.2);
            border-radius: 15px;
            padding: 20px;
            margin: 20px 0;
            border-left: 4px solid #FFB300;
        }}
        .score {{
            font-size: 2em;
            font-weight: bold;
            color: #4CAF50;
            text-align: center;
            margin: 10px 0;
        }}
        .metric {{
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid rgba(255,255,255,0.2);
        }}
        .metric:last-child {{ border-bottom: none; }}
        .insight {{
            background: rgba(33, 150, 243, 0.2);
            padding: 15px;
            border-radius: 10px;
            margin: 10px 0;
        }}
        .button {{
            background: #FFB300;
            color: #1976D2;
            padding: 15px 25px;
            border: none;
            border-radius: 25px;
            font-weight: bold;
            cursor: pointer;
            margin: 10px;
            text-decoration: none;
            display: inline-block;
            transition: transform 0.2s;
        }}
        .button:hover {{ transform: scale(1.05); }}
        .info-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin: 20px 0;
        }}
        @media (max-width: 600px) {{
            .container {{ padding: 20px; }}
            h1 {{ font-size: 2em; }}
            .info-grid {{ grid-template-columns: 1fr; }}
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>🏌️ SwingSync AI</h1>
        <p class="subtitle">Professional Golf Coaching - Mobile Demo</p>
        
        <div class="status">
            <h3>✅ System Status: Online</h3>
            <p><strong>Device IP:</strong> {local_ip}</p>
            <p><strong>Version:</strong> {self.version}</p>
            <p><strong>Platform:</strong> {device_info.get('platform', 'Mobile')}</p>
        </div>
        
        <div class="demo-result">
            <h3>🎯 Demo Swing Analysis</h3>
            <div class="score">{demo_analysis['analysis_results']['overall_score']}%</div>
            <p style="text-align: center; margin-bottom: 20px;"><strong>Overall Swing Score</strong></p>
            
            <div class="metric">
                <span>X-Factor (Power):</span>
                <span><strong>{demo_analysis['analysis_results']['x_factor']}°</strong></span>
            </div>
            <div class="metric">
                <span>Tempo Score:</span>
                <span><strong>{demo_analysis['analysis_results']['tempo_score']}%</strong></span>
            </div>
            <div class="metric">
                <span>Balance Score:</span>
                <span><strong>{demo_analysis['analysis_results']['balance_score']}%</strong></span>
            </div>
            <div class="metric">
                <span>Power Rating:</span>
                <span><strong>{demo_analysis['analysis_results']['power_rating']}</strong></span>
            </div>
            
            <h4 style="margin-top: 20px;">🔍 Key Insights:</h4>"""
        
        for insight in demo_analysis['analysis_results']['key_insights']:
            html += f'<div class="insight">• {insight}</div>'
        
        html += f"""
            <h4 style="margin-top: 20px;">🗣️ AI Coach Says:</h4>
            <div class="insight">{demo_analysis['coaching_feedback']}</div>
        </div>
        
        <h3>🚀 Available Features:</h3>
        <div class="info-grid">"""
        
        for feature in self.features:
            html += f'<div class="feature"><strong>✅ {feature}</strong></div>'
        
        html += f"""
        </div>
        
        <div style="text-align: center; margin-top: 30px;">
            <a href="#" class="button" onclick="refreshDemo()">🔄 New Demo Analysis</a>
            <a href="tel:+1234567890" class="button">📞 Contact Support</a>
        </div>
        
        <div style="text-align: center; margin-top: 40px; opacity: 0.7;">
            <p>🌐 Access from any device on your WiFi network</p>
            <p>📱 This is a demo - build the full Android app for complete features</p>
            <p>⏰ Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>
    </div>
    
    <script>
        function refreshDemo() {{
            location.reload();
        }}
    </script>
</body>
</html>"""
        return html

def main():
    """Main demo function"""
    print("🏌️ SwingSync AI Mobile Demo")
    print("=" * 40)
    
    demo = SwingSyncMobileDemo()
    
    # Get device info
    device_info = demo.get_device_info()
    local_ip = demo.get_local_ip()
    
    print(f"📱 Device IP: {local_ip}")
    print(f"🖥️  Platform: {device_info.get('platform', 'Unknown')}")
    print()
    
    # Generate demo analysis
    demo_analysis = demo.demo_swing_analysis()
    print("🎯 Demo Swing Analysis:")
    print(f"   Overall Score: {demo_analysis['analysis_results']['overall_score']}%")
    print(f"   X-Factor: {demo_analysis['analysis_results']['x_factor']}°")
    print(f"   Power Rating: {demo_analysis['analysis_results']['power_rating']}")
    print()
    
    # Save mobile UI
    html_content = demo.generate_mobile_ui()
    with open("mobile_demo.html", "w") as f:
        f.write(html_content)
    
    print("✅ Mobile demo created: mobile_demo.html")
    print()
    print("🌐 To view the demo:")
    print("   1. Open mobile_demo.html in your browser")
    print("   2. Or share the file with other devices")
    print("   3. Access your analysis and features")
    print()
    print("🚀 For full functionality, start the main server:")
    print("   python -m uvicorn main:app --host 0.0.0.0 --port 8000")

if __name__ == "__main__":
    main()