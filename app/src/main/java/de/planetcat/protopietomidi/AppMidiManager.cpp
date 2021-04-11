AMidiDevice midiDevice;
static pthread_t readThread;

static const AMidiDevice* midiDevice = AMIDI_INVALID_HANDLE;
static std::atomic<AMidiOutputPort*> midiOutputPort(AMIDI_INVALID_HANDLE);

void Java_com_nativemidiapp_AppMidiManager_startReadingMidi(
        JNIEnv* env, jobject, jobject deviceObj, jint portNumber) {
    AMidiDevice_fromJava(j_env, deviceObj, &midiDevice);

    AMidiOutputPort* outputPort;
    int32_t result =
      AMidiOutputPort_open(midiDevice, portNumber, &outputPort);
    // check for errors...

    // Start read thread
    int pthread_result =
      pthread_create(&readThread, NULL, readThreadRoutine, NULL);
    // check for errors...

}

void* readThreadRoutine(void * /*context*/) {
    uint8_t inDataBuffer[SIZE_DATABUFFER];
    int32_t numMessages;
    uint32_t opCode;
    uint64_t timestamp;
    reading = true;
    while (reading) {
        AMidiOutputPort* outputPort = midiOutputPort.load();
        numMessages =
              AMidiOutputPort_receive(outputPort, &opCode, inDataBuffer,
                                sizeof(inDataBuffer), &timestamp);
        if (numMessages >= 0) {
            if (opCode == AMIDI_OPCODE_DATA) {
                // Dispatch the MIDI data….
            }
        } else {
            // some error occurred, the negative numMessages is the error code
            int32_t errorCode = numMessages;
        }
  }
}

