package de.planetcat.protopietomidi

class MidiCmd(private val status: Status, private val channel: Int, private val value1: Int, val value2: Int) {
    override fun toString(): String {
        return status.name+"-"+channel+"-"+value1
    }
}