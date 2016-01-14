package org.jsresources.audioloop;

/*
 *	AudioLoop.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2003 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 |<---            this code is formatted to fit into 80 columns             --->|
 */
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.AudioFileFormat;

/*	If the compilation fails because this class is not available,
 get gnu.getopt from the URL given in the comment below.
 */
import gnu.getopt.Getopt;

// TODO: params for audio quality, optionally use compression and decompression in the loop (see ~/AudioLoop.java)
/**
 * <titleabbrev>AudioLoop</titleabbrev>
 * <title>Recording and playing back the recorded data immediately</title>
 *
	<formalpara><title>Purpose</title>
 * <para>
 * This program opens two lines: one for recording and one for playback. In an
 * infinite loop, it reads data from the recording line and writes them to the
 * playback line. You can use this to measure the delays inside Java Sound:
 * Speak into the microphone and wait untill you hear yourself in the speakers.
 * This can be used to experience the effect of changing the buffer sizes: use
 * the <option>-e</option> and <option>-i</option> options. You will notice that
 * the delays change, too.
 * </para></formalpara>
 *
	<formalpara><title>Usage</title>
 * <para>
 * <cmdsynopsis>
 * <command>java AudioLoop</command>
 * <arg choice="plain"><option>-l</option></arg>
 * </cmdsynopsis>
 * <cmdsynopsis>
 * <command>java AudioLoop</command>
 * <arg><option>-M <replaceable>mixername</replaceable></option></arg>
 * <arg><option>-e <replaceable>buffersize</replaceable></option></arg>
 * <arg><option>-i <replaceable>buffersize</replaceable></option></arg>
 * </cmdsynopsis>
 * </para></formalpara>
 *
	<formalpara><title>Parameters</title>
 * <variablelist>
 * <varlistentry>
 * <term><option>-l</option></term>
 * <listitem><para>lists the available mixers</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><option>-M <replaceable>mixername</replaceable></option></term>
 * <listitem><para>selects a mixer to play on</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><option>-e <replaceable>buffersize</replaceable></option></term>
 * <listitem><para>the buffer size to use in the application
 * ("extern")</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><option>-i <replaceable>buffersize</replaceable></option></term>
 * <listitem><para>the buffer size to use in Java Sound
 * ("intern")</para></listitem>
 * </varlistentry>
 * </variablelist>
 * </formalpara>
 *
	<formalpara><title>Bugs, limitations</title>
 * <para>
 * There is no way to stop the program besides brute force (ctrl-C). There is no
 * way to set the audio quality.
 * </para>
 *
	<para>The example requires that the soundcard and its driver as well as the
 * Java Sound implementation support full-duplex operation. In Linux either use <ulink
 * url="http://www.tritonus.org/">Tritonus</ulink> or enable full-duplex in
 * Sun's Java Sound implementation (search the archive of java-linux).</para>
 * </formalpara>
 *
	<formalpara><title>Source code</title>
 * <para>
 * <ulink url="AudioLoop.java.html">AudioLoop.java</ulink>,
 * <ulink url="AudioCommon.java.html">AudioCommon.java</ulink>,
 * <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
 * </para>
 * </formalpara>
 */
public class AudioLoop
        extends Thread {

    /**
     * Flag for debugging messages. If true, some messages are dumped to the
     * console during operation.
     */
    private static boolean DEBUG;

    private static final int DEFAULT_INTERNAL_BUFSIZ = 40960;
    private static final int DEFAULT_EXTERNAL_BUFSIZ = 40960;

    private TargetDataLine m_targetLine;
    private SourceDataLine m_sourceLine;
    private boolean m_bRecording;
    private int m_nExternalBufferSize;


    /*
     *	We have to pass an AudioFormat to describe in which
     *	format the audio data should be recorded and played.
     */
    public AudioLoop(AudioFormat format,
            int nInternalBufferSize,
            int nExternalBufferSize,
            String strMixerName)
            throws LineUnavailableException {
        Mixer mixer = null;
        if (strMixerName != null) {
            Mixer.Info mixerInfo = AudioCommon.getMixerInfo(strMixerName);
            if (DEBUG) {
                out("AudioLoop.<init>(): mixer info: " + mixerInfo);
            }
            mixer = AudioSystem.getMixer(mixerInfo);
            if (DEBUG) {
                out("AudioLoop.<init>(): mixer: " + mixer);
            }
        }
        /*
         *	We retrieve and open the recording and the playback line.
         */
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format, nInternalBufferSize);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format, nInternalBufferSize);
        if (mixer != null) {
            m_targetLine = (TargetDataLine) mixer.getLine(targetInfo);
            m_sourceLine = (SourceDataLine) mixer.getLine(sourceInfo);
        } else {
            m_targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            m_sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
        }
        if (DEBUG) {
            out("AudioLoop.<init>(): SourceDataLine: " + m_sourceLine);
        }
        if (DEBUG) {
            out("AudioLoop.<init>(): TargetDataLine: " + m_targetLine);
        }
        m_targetLine.open(format, nInternalBufferSize);
        m_sourceLine.open(format, nInternalBufferSize);
        m_nExternalBufferSize = nExternalBufferSize;
    }

    public void start() {
        m_targetLine.start();
        m_sourceLine.start();
        // start thread
        super.start();
    }


    /*
     public void stopRecording()
     {
     m_line.stop();
     m_line.close();
     m_bRecording = false;
     }
     */
    public void run() {
        byte[] abBuffer = new byte[m_nExternalBufferSize];
        int nBufferSize = abBuffer.length;
        m_bRecording = true;
        while (m_bRecording) {
            if (DEBUG) {
                out("Trying to read: " + nBufferSize);
            }
            /*
             *	read a block of data from the recording line.
             */
            int nBytesRead = m_targetLine.read(abBuffer, 0, nBufferSize);
            if (DEBUG) {
                out("Read: " + nBytesRead);
            }
            /*
             *	And now, we write the block to the playback
             *	line.
             */
            m_sourceLine.write(abBuffer, 0, nBytesRead);
        }
    }

    public static void main(String[] args) {
        String strMixerName = null;
        float fFrameRate = 44100.0F;
        int nInternalBufferSize = DEFAULT_INTERNAL_BUFSIZ;
        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFSIZ;

        Getopt g = new Getopt("AudioLoop", args, "hlr:i:e:M:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    printUsageAndExit();

                case 'l':
                    AudioCommon.listMixersAndExit();

                case 'r':
                    fFrameRate = Float.parseFloat(g.getOptarg());
                    if (DEBUG) {
                        out("AudioLoop.main(): frame rate: " + fFrameRate);
                    }
                    break;

                case 'i':
                    nInternalBufferSize = Integer.parseInt(g.getOptarg());
                    if (DEBUG) {
                        out("AudioLoop.main(): internal buffer size: " + nInternalBufferSize);
                    }
                    break;

                case 'e':
                    nExternalBufferSize = Integer.parseInt(g.getOptarg());
                    if (DEBUG) {
                        out("AudioLoop.main(): external buffer size: " + nExternalBufferSize);
                    }
                    break;

                case 'M':
                    strMixerName = g.getOptarg();
                    if (DEBUG) {
                        out("AudioLoop.main(): mixer name: " + strMixerName);
                    }
                    break;

                case 'D':
                    DEBUG = true;
                    break;

                case '?':
                    printUsageAndExit();

                default:
                    out("AudioLoop.main(): getopt() returned: " + c);
                    break;
            }
        }
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fFrameRate, 16, 2, 4, fFrameRate, false);
        if (DEBUG) {
            out("AudioLoop.main(): audio format: " + audioFormat);
        }
        AudioLoop audioLoop = null;
        try {
            audioLoop = new AudioLoop(audioFormat,
                    nInternalBufferSize,
                    nExternalBufferSize,
                    strMixerName);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        audioLoop.start();
    }

    private static void printUsageAndExit() {
        out("AudioLoop: usage:");
        out("\tjava AudioLoop -h");
        out("\tjava AudioLoop -l");
        out("\tjava AudioLoop [-D] [-M <mixername>] [-e <buffersize>] [-i <buffersize>]");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}



/*** AudioLoop.java ***/
