package org.jsresources.audioconcat;

/*
 *	AudioConcat.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
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
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/*	If the compilation fails because this class is not available,
 get gnu.getopt from the URL given in the comment below.
 */
import gnu.getopt.Getopt;

// TODO: the name AudioConcat is no longer appropriate. There should be a name that is neutral to concat/mix.
/**
 * <titleabbrev>AudioConcat</titleabbrev>
 * <title>Concatenating or mixing audio files</title>
 *
	<formalpara><title>Purpose</title>
 * <para>This program reads multiple audio files and writes a single one either
 * containing the data of all the other files in order (concatenation mode,
 * option <option>-c</option>) or containing a mixdown of all the other files
 * (mixing mode, option <option>-m</option> or option <option>-f</option>). For
 * concatenation, the input files must have the same audio format. They need not
 * have the same file type.</para>
 * </formalpara>
 *
	<formalpara><title>Usage</title>
 * <para>
 * <cmdsynopsis>
 * <command>java AudioConcat</command>
 * <arg choice="plain"><option>-h</option></arg>
 * </cmdsynopsis>
 * <cmdsynopsis>
 * <command>java AudioConcat</command>
 * <arg choice="opt"><option>-D</option></arg>
 * <group choice="plain">
 * <arg><option>-c</option></arg>
 * <arg><option>-m</option></arg>
 * <arg><option>-f</option></arg>
 * </group>
 * <arg choice="plain"><option>-o
 * <replaceable>outputfile</replaceable></option></arg>
 * <arg choice="plain" rep="repeat"><replaceable>inputfile</replaceable></arg>
 * </cmdsynopsis>
 * </para>
 * </formalpara>
 *
	<formalpara><title>Parameters</title>
 * <variablelist>
 * <varlistentry>
 * <term><option>-c</option></term>
 * <listitem><para>selects concatenation mode</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><option>-m</option></term>
 * <listitem><para>selects mixing mode</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><option>-f</option></term>
 * <listitem><para>selects float mixing mode</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><option>-o <replaceable>outputfile</replaceable></option></term>
 * <listitem><para>The filename of the output file</para></listitem>
 * </varlistentry>
 * <varlistentry>
 * <term><replaceable>inputfile</replaceable></term>
 * <listitem><para>the name(s) of input file(s)</para></listitem>
 * </varlistentry>
 * </variablelist>
 * </formalpara>
 *
	<formalpara><title>Bugs, limitations</title>
 * <para>
 * This program is not well-tested. Output is always a WAV file. Future versions
 * should be able to convert different audio formats to a dedicated target
 * format.
 * </para></formalpara>
 *
	<formalpara><title>Source code</title>
 * <para>
 * <ulink url="AudioConcat.java.html">AudioConcat.java</ulink>,
 * <ulink url="SequenceAudioInputStream.java.html">SequenceAudioInputStream.java</ulink>,
 * <ulink url="MixingAudioInputStream.java.html">MixingAudioInputStream.java</ulink>,
 * <ulink url="MixingFloatAudioInputStream.java.html">MixingFloatAudioInputStream.java</ulink>,
 * <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
 * </para>
 * </formalpara>
 *
 */
public class AudioConcat {

    private static final int MODE_NONE = 0;
    private static final int MODE_MIXING = 1;
    private static final int MODE_FLOATMIXING = 2;
    private static final int MODE_CONCATENATION = 3;

    /**
     * Flag for debugging messages. If true, some messages are dumped to the
     * console during operation.
     */
    private static boolean DEBUG = false;

    public static void main(String[] args) {
        /**
         * Mode of operation. Determines what to do with the input files: either
         * mixing or concatenation.
         */
        int nMode = MODE_NONE;
        String strOutputFilename = null;
        AudioFormat audioFormat = null;
        List audioInputStreamList = new ArrayList();

		// int	nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
        // int	nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
        /*
         *	Parsing of command-line options takes place...
         */
        Getopt g = new Getopt("AudioConcat", args, "hDcmfo:");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    printUsageAndExit();

                case 'o':
                    strOutputFilename = g.getOptarg();
                    if (DEBUG) {
                        out("AudioConcat.main(): output filename: " + strOutputFilename);
                    }
                    break;

                case 'c':
                    nMode = MODE_CONCATENATION;
                    break;

                case 'm':
                    nMode = MODE_MIXING;
                    break;

                case 'f':
                    nMode = MODE_FLOATMIXING;
                    break;

                case 'D':
                    DEBUG = true;
                    break;

                case '?':
                    printUsageAndExit();

                default:
                    out("AudioConcat.main(): getopt() returned " + c);
                    break;
            }
        }

        /*
         *	All remaining arguments are assumed to be filenames of
         *	soundfiles we want to play.
         */
        String strFilename = null;
        for (int i = g.getOptind(); i < args.length; i++) {
            strFilename = args[i];
            File soundFile = new File(strFilename);

            /*
             *	We have to read in the sound file.
             */
            AudioInputStream audioInputStream = null;
            try {
                audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            } catch (Exception e) {
                /*
                 *	In case of an exception, we dump the exception
                 *	including the stack trace to the console output.
                 *	Then, we exit the program.
                 */
                e.printStackTrace();
                System.exit(1);
            }
            AudioFormat format = audioInputStream.getFormat();
            /*
             The first input file determines the audio format. This stream's
             AudioFormat is stored. All other streams are checked against
             this format.
             */
            if (audioFormat == null) {
                audioFormat = format;
                if (DEBUG) {
                    out("AudioConcat.main(): format: " + audioFormat);
                }
            } else if (!audioFormat.matches(format)) {
                // TODO: try to convert
                out("AudioConcat.main(): WARNING: AudioFormats don't match");
                out("AudioConcat.main(): master format: " + audioFormat);
                out("AudioConcat.main(): this format: " + format);
            }
            audioInputStreamList.add(audioInputStream);
        }

        if (audioFormat == null) {
            out("No input filenames!");
            printUsageAndExit();
        }
        AudioInputStream audioInputStream = null;
        switch (nMode) {
            case MODE_CONCATENATION:
                audioInputStream = new SequenceAudioInputStream(audioFormat, audioInputStreamList);
                break;

            case MODE_MIXING:
                audioInputStream = new MixingAudioInputStream(audioFormat, audioInputStreamList);
                break;

            case MODE_FLOATMIXING:
                audioInputStream = new MixingFloatAudioInputStream(audioFormat, audioInputStreamList);
                break;

            default:
                out("you have to specify a mode (either -m or -c).");
                printUsageAndExit();
        }

        if (strOutputFilename == null) {
            out("you have to specify an output filename (using -o <filename>).");
            printUsageAndExit();
        }
        File outputFile = new File(strOutputFilename);
        try {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (DEBUG) {
            out("AudioConcat.main(): before exit");
        }
        System.exit(0);
    }

    private static void printUsageAndExit() {
        out("AudioConcat: usage:");
        out("\tjava AudioConcat -h");
        out("\tjava AudioConcat [-D] -c|-m|-f -o <outputfile> <inputfile> ...");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}



/*** AudioConcat.java ***/
