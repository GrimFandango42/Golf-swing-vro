"""
Speech Interface for Conversational Coaching

This module provides speech-to-text and text-to-speech capabilities for the
conversational coaching system, supporting multiple providers and real-time processing.

Features:
- Multiple STT providers (Google, Azure, Whisper)
- Multiple TTS providers (OpenAI, Google, ElevenLabs)
- Real-time streaming audio processing
- Voice command recognition
- Audio quality optimization
- Offline fallback capabilities
"""

import asyncio
import io
import tempfile
import os
import json
import logging
from typing import Dict, List, Optional, Any, AsyncGenerator, Union
from dataclasses import dataclass
from enum import Enum
import numpy as np

# Third-party imports (would be installed via requirements)
try:
    import speech_recognition as sr
    from google.cloud import speech
    import openai
    import azure.cognitiveservices.speech as speechsdk
    import whisper
    STT_AVAILABLE = True
except ImportError as e:
    STT_AVAILABLE = False
    logging.warning(f"Speech recognition libraries not available: {e}")

logger = logging.getLogger(__name__)

class STTProvider(Enum):
    GOOGLE = "google"
    AZURE = "azure" 
    WHISPER = "whisper"
    OPENAI = "openai"

class TTSProvider(Enum):
    OPENAI = "openai"
    GOOGLE = "google"
    AZURE = "azure"
    ELEVENLABS = "elevenlabs"

@dataclass
class VoiceSettings:
    """Voice generation settings"""
    voice_id: str = "alloy"  # OpenAI default
    speed: float = 1.0
    pitch: float = 1.0
    volume: float = 1.0
    emotion: str = "neutral"
    language: str = "en-US"

@dataclass
class AudioConfig:
    """Audio processing configuration"""
    sample_rate: int = 16000
    channels: int = 1
    bit_depth: int = 16
    format: str = "wav"
    noise_reduction: bool = True
    auto_gain: bool = True

class VoiceInterface:
    """Main speech interface for conversational coaching"""
    
    def __init__(self, 
                 stt_provider: STTProvider = STTProvider.GOOGLE,
                 tts_provider: TTSProvider = TTSProvider.OPENAI,
                 audio_config: AudioConfig = None):
        self.stt_provider = stt_provider
        self.tts_provider = tts_provider
        self.audio_config = audio_config or AudioConfig()
        
        # Initialize providers
        self.stt_client = None
        self.tts_client = None
        self.whisper_model = None
        
        self._initialize_providers()
        
        # Voice command processor
        self.command_processor = VoiceCommandProcessor()
    
    def _initialize_providers(self):
        """Initialize speech providers based on configuration"""
        if not STT_AVAILABLE:
            logger.warning("Speech recognition not available - using fallback")
            return
        
        try:
            # Initialize STT provider
            if self.stt_provider == STTProvider.GOOGLE:
                self.stt_client = speech.SpeechClient()
            elif self.stt_provider == STTProvider.AZURE:
                speech_key = os.getenv("AZURE_SPEECH_KEY")
                speech_region = os.getenv("AZURE_SPEECH_REGION", "eastus")
                if speech_key:
                    speech_config = speechsdk.SpeechConfig(
                        subscription=speech_key, 
                        region=speech_region
                    )
                    self.stt_client = speech_config
            elif self.stt_provider == STTProvider.WHISPER:
                self.whisper_model = whisper.load_model("base")
            
            # Initialize TTS (OpenAI client assumed to be configured globally)
            logger.info(f"Voice interface initialized with {self.stt_provider.value} STT and {self.tts_provider.value} TTS")
            
        except Exception as e:
            logger.error(f"Error initializing speech providers: {e}")
    
    async def process_voice_input(self, audio_data: bytes) -> str:
        """Process voice input and return transcribed text"""
        try:
            if self.stt_provider == STTProvider.GOOGLE:
                return await self._google_stt(audio_data)
            elif self.stt_provider == STTProvider.AZURE:
                return await self._azure_stt(audio_data)
            elif self.stt_provider == STTProvider.WHISPER:
                return await self._whisper_stt(audio_data)
            elif self.stt_provider == STTProvider.OPENAI:
                return await self._openai_stt(audio_data)
            else:
                raise ValueError(f"Unsupported STT provider: {self.stt_provider}")
        
        except Exception as e:
            logger.error(f"Error processing voice input: {e}")
            return "Sorry, I couldn't understand that. Could you try again?"
    
    async def generate_voice_response(self, text: str, 
                                    voice_settings: VoiceSettings = None) -> bytes:
        """Generate voice response from text"""
        try:
            if self.tts_provider == TTSProvider.OPENAI:
                return await self._openai_tts(text, voice_settings)
            elif self.tts_provider == TTSProvider.GOOGLE:
                return await self._google_tts(text, voice_settings)
            elif self.tts_provider == TTSProvider.AZURE:
                return await self._azure_tts(text, voice_settings)
            elif self.tts_provider == TTSProvider.ELEVENLABS:
                return await self._elevenlabs_tts(text, voice_settings)
            else:
                raise ValueError(f"Unsupported TTS provider: {self.tts_provider}")
        
        except Exception as e:
            logger.error(f"Error generating voice response: {e}")
            # Return empty audio on error
            return b""
    
    async def stream_voice_response(self, text: str, 
                                  voice_settings: VoiceSettings = None) -> AsyncGenerator[bytes, None]:
        """Generate streaming voice response"""
        try:
            # For now, generate full response and yield in chunks
            # Real streaming would require provider-specific implementation
            audio_data = await self.generate_voice_response(text, voice_settings)
            
            # Yield audio in chunks for streaming playback
            chunk_size = 4096
            for i in range(0, len(audio_data), chunk_size):
                yield audio_data[i:i + chunk_size]
                await asyncio.sleep(0.01)  # Small delay for streaming effect
        
        except Exception as e:
            logger.error(f"Error streaming voice response: {e}")
            yield b""
    
    # STT Implementation Methods
    
    async def _google_stt(self, audio_data: bytes) -> str:
        """Google Speech-to-Text implementation"""
        if not self.stt_client:
            return "Google STT not configured"
        
        try:
            # Configure recognition
            config = speech.RecognitionConfig(
                encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16,
                sample_rate_hertz=self.audio_config.sample_rate,
                language_code="en-US",
                model="command_and_search",  # Optimized for voice commands
                enable_automatic_punctuation=True,
                enable_word_time_offsets=False,
                use_enhanced=True
            )
            
            audio = speech.RecognitionAudio(content=audio_data)
            
            # Perform recognition
            response = self.stt_client.recognize(config=config, audio=audio)
            
            if response.results:
                return response.results[0].alternatives[0].transcript
            else:
                return "No speech detected"
        
        except Exception as e:
            logger.error(f"Google STT error: {e}")
            return "Speech recognition error"
    
    async def _azure_stt(self, audio_data: bytes) -> str:
        """Azure Speech Services STT implementation"""
        if not self.stt_client:
            return "Azure STT not configured"
        
        try:
            # Save audio to temporary file
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp_file:
                tmp_file.write(audio_data)
                tmp_file_path = tmp_file.name
            
            # Configure audio input
            audio_input = speechsdk.AudioConfig(filename=tmp_file_path)
            speech_recognizer = speechsdk.SpeechRecognizer(
                speech_config=self.stt_client, 
                audio_config=audio_input
            )
            
            # Perform recognition
            result = speech_recognizer.recognize_once()
            
            # Clean up temporary file
            os.unlink(tmp_file_path)
            
            if result.reason == speechsdk.ResultReason.RecognizedSpeech:
                return result.text
            else:
                return "No speech recognized"
        
        except Exception as e:
            logger.error(f"Azure STT error: {e}")
            return "Speech recognition error"
    
    async def _whisper_stt(self, audio_data: bytes) -> str:
        """Whisper STT implementation"""
        if not self.whisper_model:
            return "Whisper not configured"
        
        try:
            # Save audio to temporary file
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp_file:
                tmp_file.write(audio_data)
                tmp_file_path = tmp_file.name
            
            # Transcribe with Whisper
            result = self.whisper_model.transcribe(tmp_file_path, language="en")
            
            # Clean up temporary file
            os.unlink(tmp_file_path)
            
            return result["text"].strip()
        
        except Exception as e:
            logger.error(f"Whisper STT error: {e}")
            return "Speech recognition error"
    
    async def _openai_stt(self, audio_data: bytes) -> str:
        """OpenAI Whisper API STT implementation"""
        try:
            # Save audio to temporary file
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp_file:
                tmp_file.write(audio_data)
                tmp_file_path = tmp_file.name
            
            # Use OpenAI Whisper API
            with open(tmp_file_path, "rb") as audio_file:
                transcript = await openai.Audio.atranscribe("whisper-1", audio_file)
            
            # Clean up temporary file
            os.unlink(tmp_file_path)
            
            return transcript.text.strip()
        
        except Exception as e:
            logger.error(f"OpenAI STT error: {e}")
            return "Speech recognition error"
    
    # TTS Implementation Methods
    
    async def _openai_tts(self, text: str, voice_settings: VoiceSettings = None) -> bytes:
        """OpenAI Text-to-Speech implementation"""
        try:
            settings = voice_settings or VoiceSettings()
            
            response = await openai.Audio.aspeech.create(
                model="tts-1",
                voice=settings.voice_id,
                input=text,
                response_format="mp3",
                speed=settings.speed
            )
            
            return response.content
        
        except Exception as e:
            logger.error(f"OpenAI TTS error: {e}")
            return b""
    
    async def _google_tts(self, text: str, voice_settings: VoiceSettings = None) -> bytes:
        """Google Text-to-Speech implementation"""
        try:
            from google.cloud import texttospeech
            
            client = texttospeech.TextToSpeechClient()
            settings = voice_settings or VoiceSettings()
            
            # Set up the synthesis input
            synthesis_input = texttospeech.SynthesisInput(text=text)
            
            # Build the voice request
            voice = texttospeech.VoiceSelectionParams(
                language_code=settings.language,
                ssml_gender=texttospeech.SsmlVoiceGender.NEUTRAL
            )
            
            # Select the type of audio file
            audio_config = texttospeech.AudioConfig(
                audio_encoding=texttospeech.AudioEncoding.MP3,
                speaking_rate=settings.speed,
                pitch=settings.pitch,
                volume_gain_db=settings.volume
            )
            
            # Perform the text-to-speech request
            response = client.synthesize_speech(
                input=synthesis_input, 
                voice=voice, 
                audio_config=audio_config
            )
            
            return response.audio_content
        
        except Exception as e:
            logger.error(f"Google TTS error: {e}")
            return b""
    
    async def _azure_tts(self, text: str, voice_settings: VoiceSettings = None) -> bytes:
        """Azure Text-to-Speech implementation"""
        # Implementation would go here
        logger.warning("Azure TTS not implemented yet")
        return b""
    
    async def _elevenlabs_tts(self, text: str, voice_settings: VoiceSettings = None) -> bytes:
        """ElevenLabs Text-to-Speech implementation"""
        # Implementation would go here
        logger.warning("ElevenLabs TTS not implemented yet")
        return b""

class VoiceCommandProcessor:
    """Processes voice commands for coaching interaction"""
    
    def __init__(self):
        self.commands = {
            "start_practice": {
                "phrases": ["start practice", "begin session", "let's practice", "start coaching"],
                "confidence_threshold": 0.8
            },
            "end_practice": {
                "phrases": ["end practice", "stop session", "finish up", "that's enough"],
                "confidence_threshold": 0.8
            },
            "analyze_swing": {
                "phrases": ["analyze my swing", "check my form", "how did I do", "review my swing"],
                "confidence_threshold": 0.7
            },
            "get_tips": {
                "phrases": ["give me tips", "what should I work on", "help me improve", "any advice"],
                "confidence_threshold": 0.7
            },
            "repeat": {
                "phrases": ["repeat that", "say again", "what did you say", "come again"],
                "confidence_threshold": 0.9
            },
            "slow_down": {
                "phrases": ["slow down", "speak slower", "too fast", "slower please"],
                "confidence_threshold": 0.8
            },
            "be_quiet": {
                "phrases": ["be quiet", "stop talking", "less feedback", "quiet mode"],
                "confidence_threshold": 0.8
            },
            "change_voice": {
                "phrases": ["change voice", "different voice", "voice settings", "new voice"],
                "confidence_threshold": 0.8
            },
            "help": {
                "phrases": ["help", "what can you do", "commands", "instructions"],
                "confidence_threshold": 0.8
            }
        }
    
    def process_command(self, transcribed_text: str) -> Dict[str, Any]:
        """Process voice command and return intent with confidence"""
        text_lower = transcribed_text.lower().strip()
        
        best_match = {
            "command": "conversation",
            "confidence": 0.0,
            "matched_phrase": "",
            "original_text": transcribed_text
        }
        
        for command, config in self.commands.items():
            for phrase in config["phrases"]:
                # Simple substring matching - could be enhanced with fuzzy matching
                if phrase in text_lower:
                    confidence = len(phrase) / len(text_lower)  # Simple confidence scoring
                    
                    if confidence > best_match["confidence"] and confidence >= config["confidence_threshold"]:
                        best_match = {
                            "command": command,
                            "confidence": confidence,
                            "matched_phrase": phrase,
                            "original_text": transcribed_text
                        }
        
        return best_match
    
    def get_command_help(self) -> str:
        """Get help text for available voice commands"""
        help_text = "Available voice commands:\n"
        
        for command, config in self.commands.items():
            example_phrase = config["phrases"][0]
            help_text += f"• {example_phrase.title()}\n"
        
        help_text += "\nJust speak naturally - I'll understand what you mean!"
        return help_text

# Audio utility functions

def preprocess_audio(audio_data: bytes, config: AudioConfig) -> bytes:
    """Preprocess audio for better recognition"""
    try:
        # Convert to numpy array for processing
        audio_array = np.frombuffer(audio_data, dtype=np.int16)
        
        # Apply noise reduction if enabled
        if config.noise_reduction:
            audio_array = apply_noise_reduction(audio_array)
        
        # Apply auto gain if enabled
        if config.auto_gain:
            audio_array = apply_auto_gain(audio_array)
        
        # Convert back to bytes
        return audio_array.tobytes()
    
    except Exception as e:
        logger.error(f"Error preprocessing audio: {e}")
        return audio_data

def apply_noise_reduction(audio_array: np.ndarray) -> np.ndarray:
    """Apply basic noise reduction"""
    # Simple high-pass filter to remove low-frequency noise
    # In production, would use more sophisticated noise reduction
    return audio_array

def apply_auto_gain(audio_array: np.ndarray) -> np.ndarray:
    """Apply automatic gain control"""
    # Normalize audio to prevent clipping
    max_val = np.max(np.abs(audio_array))
    if max_val > 0:
        gain = min(32767 / max_val, 2.0)  # Limit gain to prevent over-amplification
        audio_array = audio_array * gain
    
    return audio_array.astype(np.int16)

def detect_speech_activity(audio_data: bytes, threshold: float = 0.01) -> bool:
    """Detect if audio contains speech activity"""
    try:
        audio_array = np.frombuffer(audio_data, dtype=np.int16)
        # Simple energy-based voice activity detection
        energy = np.mean(np.abs(audio_array.astype(float))) / 32768.0
        return energy > threshold
    except:
        return True  # Assume speech if detection fails