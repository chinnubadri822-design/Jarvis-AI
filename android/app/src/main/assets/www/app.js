const $ = (id) => document.getElementById(id);
const input = $("commandInput");
const conversation = $("conversation");
const reactor = $("reactor");
const waveform = $("waveform");
let recognition = null;
let listening = false;
let audioContext = null;
let clapStream = null;
let clapEnabled = false;
let wakeEnabled = false;
let clapCooldown = 0;

function updateClock() {
  const now = new Date();
  $("clock").textContent = now.toLocaleTimeString([], {hour12:false});
  $("footerDate").textContent = now.toLocaleDateString([], {year:"numeric",month:"short",day:"2-digit"}).toUpperCase();
}
setInterval(updateClock, 1000); updateClock();

function toast(message) {
  const el = $("toast"); el.textContent = message; el.classList.add("show");
  clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove("show"), 3200);
}
function setState(active, label) {
  listening = active;
  reactor.classList.toggle("active", active);
  waveform.classList.toggle("active", active);
  $("listeningState").textContent = label || (active ? "LISTENING" : "AWAITING INPUT");
  $("systemStatus").textContent = active ? "VOICE SESSION ACTIVE" : "SYSTEM STANDBY";
  $("coreHint").textContent = active ? "I'm listening. Speak your command." : "Tap the microphone to start a session";
  $("voiceMetric").textContent = active ? "LISTENING" : "READY";
}
function addMessage(text, who="jarvis") {
  const wrap = document.createElement("div"); wrap.className = "message " + (who === "user" ? "user-message" : "jarvis-message");
  const avatar = document.createElement("div"); avatar.className = "avatar"; avatar.textContent = who === "user" ? "U" : "J";
  const body = document.createElement("div"); const author = document.createElement("span"); author.className = "message-author";
  author.textContent = who === "user" ? "YOU " : "JARVIS ";
  const time = document.createElement("small"); time.textContent = new Date().toLocaleTimeString([], {hour:"2-digit",minute:"2-digit"}); author.append(time);
  const p = document.createElement("p"); p.textContent = text; body.append(author,p); wrap.append(avatar,body); conversation.append(wrap); conversation.scrollTop = conversation.scrollHeight;
}
function speak(text) {
  if (!("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = 0.96; utterance.pitch = 0.88; utterance.volume = 0.9;
  window.speechSynthesis.speak(utterance);
}
function localReply(text) {
  const q = text.toLowerCase();
  if (/hello|hi jarvis|hey jarvis/.test(q)) return "Hello. JARVIS interface is online and ready to assist.";
  if (/time/.test(q)) return "The current local time is " + new Date().toLocaleTimeString() + ".";
  if (/date|today/.test(q)) return "Today is " + new Date().toLocaleDateString(undefined,{weekday:"long",year:"numeric",month:"long",day:"numeric"}) + ".";
  if (/capabilit|what can you do|help/.test(q)) return "This build includes the command interface, voice input, spoken replies, and session conversation log. On Android, you can enable offline Hey JARVIS wake-word detection from the dashboard. The local conversational AI engine is still a future milestone.";
  if (/briefing|system/.test(q)) return "JARVIS user interface is operational. Voice features depend on browser support and microphone permission. Local AI engine is not connected yet.";
  if (/plan|task/.test(q)) return "Let's break that into small steps. Tell me the goal, deadline, and what you have already completed.";
  if (/code|developer/.test(q)) return "Developer module selected. Tell me the language, the feature you want, and any error message.";
  if (/creative|idea|brainstorm/.test(q)) return "Creative module ready. Describe the idea and the style you want to explore.";
  return "I received your command: " + text + ". The interface is working, but a local language model has not been installed yet. This build does not send your message to an online AI service.";
}
function submitCommand(text) {
  const command = (text || input.value).trim(); if (!command) return;
  addMessage(command,"user"); input.value = "";
  if (window.JarvisNative && window.JarvisNative.isAndroidApp() && window.JarvisNative.isLocalAiLoaded() && window.JarvisNative.askLocalAI(command)) return;
  const reply = localReply(command);
  setTimeout(() => { addMessage(reply); speak(reply); }, 280);
}
$("sendButton").addEventListener("click", () => submitCommand());
input.addEventListener("keydown", e => { if(e.key === "Enter") submitCommand(); });
document.querySelectorAll("[data-prompt]").forEach(button => button.addEventListener("click", () => submitCommand(button.dataset.prompt)));
$("clearButton").addEventListener("click", () => { conversation.innerHTML = ""; addMessage("Conversation cleared. Ready for a new command."); });
$("settingsButton").addEventListener("click", () => toast("Settings panel will be added in the next build."));
$("modelButton").addEventListener("click", () => {
  if (!window.JarvisNative || !window.JarvisNative.isAndroidApp()) { toast("Load a local model from the Android app."); return; }
  window.JarvisNative.chooseLocalModel();
});

function startVoice() {
  if (window.JarvisNative && window.JarvisNative.isAndroidApp()) {
    if (window.JarvisNative.startVoiceRecognition()) return;
    toast("Allow microphone access, then tap the microphone again.");
    return;
  }
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) { toast("Speech recognition is not supported in this browser. Try a current Chrome browser."); return; }
  if (recognition && listening) { recognition.stop(); setState(false); return; }
  recognition = new SpeechRecognition(); recognition.lang = navigator.language || "en-US"; recognition.continuous = false; recognition.interimResults = true;
  recognition.onstart = () => setState(true,"LISTENING");
  recognition.onresult = event => {
    const result = event.results[event.results.length-1];
    if (result.isFinal) { setState(false); submitCommand(result[0].transcript); }
    else $("coreHint").textContent = result[0].transcript;
  };
  recognition.onerror = event => { setState(false); toast("Voice input: " + event.error); };
  recognition.onend = () => { if(listening) setState(false); };
  try { recognition.start(); } catch(error) { setState(false); toast("Could not start microphone. Check browser permission."); }
}
$("micButton").addEventListener("click", startVoice);
// Native callbacks from the on-device language model.
window.jarvisLocalAiState = (message) => {
  $("aiMetric").textContent = message === "LOCAL AI ONLINE" ? "ONLINE" : "LOADING";
  $("coreHint").textContent = message;
};
window.jarvisLocalAiReply = (message) => {
  addMessage(message);
  speak(message);
  $("aiMetric").textContent = "ONLINE";
  $("coreHint").textContent = "Local model response complete";
};
window.jarvisLocalAiError = (message) => {
  $("aiMetric").textContent = "NOT READY";
  toast(message);
};

// Callbacks from the native Android speech recognizer.
window.jarvisNativeVoiceState = (message) => { setState(true, "LISTENING"); $("coreHint").textContent = message; };
window.jarvisNativeVoiceResult = (phrase) => { setState(false); submitCommand(phrase); };
window.jarvisNativeVoiceError = (message) => { setState(false); toast(message); };
window.jarvisWakeDetected = () => {
  wakeEnabled = false;
  $("wakeButton").classList.remove("enabled");
  $("wakeButton").textContent = "HEY JARVIS";
  $("coreHint").textContent = "Wake phrase detected. Speak your command.";
  toast("JARVIS is ready. Listening for your command.");
  setTimeout(() => startVoice(), 650);
};


// Clap detection is opt-in and runs only while this page is open.
async function enableClapDetection() {
  if (clapEnabled) { disableClapDetection(); return; }
  if (!navigator.mediaDevices?.getUserMedia) { toast("Microphone access requires HTTPS and a supported browser."); return; }
  try {
    clapStream = await navigator.mediaDevices.getUserMedia({audio:{echoCancellation:false,noiseSuppression:false,autoGainControl:false}});
    const AudioCtx = window.AudioContext || window.webkitAudioContext; audioContext = new AudioCtx();
    const source = audioContext.createMediaStreamSource(clapStream); const analyser = audioContext.createAnalyser(); analyser.fftSize = 512; source.connect(analyser);
    const data = new Uint8Array(analyser.fftSize); let lastPeak = 0;
    clapEnabled = true; $("coreHint").textContent = "Clap detection enabled while this page remains open"; toast("Clap detection enabled. Keep this page open.");
    const detect = () => {
      if (!clapEnabled) return;
      analyser.getByteTimeDomainData(data); let peak = 0;
      for (let i=0;i<data.length;i++) peak = Math.max(peak,Math.abs(data[i]-128));
      const now = Date.now();
      if (peak > 43 && lastPeak < 24 && now-clapCooldown > 1300) { clapCooldown=now; toast("Clap detected. Tap the microphone to speak."); $("listeningState").textContent="CLAP DETECTED"; speak("Yes. I'm ready."); }
      lastPeak = peak; requestAnimationFrame(detect);
    };
    detect();
  } catch(error) { toast("Microphone permission was not granted."); }
}
function disableClapDetection() {
  clapEnabled=false; if(clapStream) clapStream.getTracks().forEach(track=>track.stop());
  if(audioContext) audioContext.close(); clapStream=null; audioContext=null;
  $("coreHint").textContent="Clap detection disabled"; toast("Clap detection disabled.");
}
$("wakeButton").addEventListener("click", () => {
  if (!window.JarvisNative || !window.JarvisNative.isAndroidApp()) {
    toast("Offline background wake-word is available in the Android app.");
    return;
  }
  if (wakeEnabled) {
    window.JarvisNative.stopHeyJarvisService();
    wakeEnabled = false;
    $("wakeButton").classList.remove("enabled");
    $("wakeButton").textContent = "HEY JARVIS";
    $("coreHint").textContent = "Hey JARVIS listener stopped";
    toast("Wake-word listener stopped.");
    return;
  }
  if (clapEnabled) {
    window.JarvisNative.stopClapService();
    clapEnabled = false;
    $("clapButton").classList.remove("enabled");
    $("clapButton").textContent = "CLAP";
  }
  if (window.JarvisNative.startHeyJarvisService()) {
    wakeEnabled = true;
    $("wakeButton").classList.add("enabled");
    $("wakeButton").textContent = "WAKE ON";
    $("coreHint").textContent = "Say “Hey JARVIS”. The microphone notification stays visible.";
    toast("Offline Hey JARVIS listener started.");
  } else {
    toast("Grant microphone permission, then tap HEY JARVIS again.");
  }
});
$("clapButton").addEventListener("click", async () => {
  if (window.JarvisNative && window.JarvisNative.isAndroidApp()) {
    if (clapEnabled) {
      window.JarvisNative.stopClapService();
      clapEnabled = false;
      $("clapButton").classList.remove("enabled");
      $("clapButton").textContent = "CLAP";
      $("coreHint").textContent = "Background clap listener stopped";
      toast("JARVIS stopped listening.");
    } else {
      if (wakeEnabled) {
        window.JarvisNative.stopHeyJarvisService();
        wakeEnabled = false;
        $("wakeButton").classList.remove("enabled");
        $("wakeButton").textContent = "HEY JARVIS";
      }
      const started = window.JarvisNative.startClapService();
      if (started) {
        clapEnabled = true;
        $("clapButton").classList.add("enabled");
        $("clapButton").textContent = "CLAP ON";
        $("coreHint").textContent = "Android foreground service is listening. Use its notification to stop.";
        toast("Clap listener started. A persistent notification will remain visible.");
      } else {
        toast("Grant microphone permission, then tap CLAP again.");
      }
    }
    return;
  }
  if (clapEnabled) {
    disableClapDetection();
    $("clapButton").classList.remove("enabled");
    $("clapButton").textContent = "CLAP";
  } else {
    await enableClapDetection();
    if (clapEnabled) {
      $("clapButton").classList.add("enabled");
      $("clapButton").textContent = "CLAP ON";
    }
  }
});

if ("serviceWorker" in navigator && location.protocol.startsWith("http")) {
  window.addEventListener("load", () => navigator.serviceWorker.register("./sw.js").catch(() => {}));
}


if (!window.isSecureContext) toast("For microphone access, open this site over HTTPS (GitHub Pages or Codespaces preview).");
