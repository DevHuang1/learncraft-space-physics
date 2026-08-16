import math
import os
import struct
import wave

OUT = "/home/ubuntu/orbital-physics-lab/android/app/src/main/res/raw"
os.makedirs(OUT, exist_ok=True)
RATE = 44100

def write_wav(path, notes, duration=0.18):
    frames = int(RATE * duration)
    data = bytearray()
    for i in range(frames):
        t = i / RATE
        envelope = math.exp(-18 * t)
        sample = sum(math.sin(2 * math.pi * freq * t) * amp for freq, amp in notes) * envelope
        value = max(-1, min(1, sample))
        data.extend(struct.pack("<h", int(value * 14000)))
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(data)

write_wav(os.path.join(OUT, "collision.wav"), [(220, 0.6), (440, 0.22)], 0.12)
write_wav(os.path.join(OUT, "gravity_well.wav"), [(110, 0.45), (330, 0.2), (660, 0.08)], 0.32)
