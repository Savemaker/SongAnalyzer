package ru.liga;


import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;
import sun.reflect.generics.tree.Tree;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class App {

    /**
     * Это пример работы, можете всё стирать и переделывать
     * Пример, чтобы убрать у вас начальный паралич разработки
     * Также посмотрите класс SongUtils, он переводит тики в миллисекунды
     * Tempo может быть только один
     */
    public static int getTextTrack(MidiFile midiFile){
        for (int i = 2;i < midiFile.getTrackCount();i++)
        {
            TreeSet<MidiEvent> eventsOfTrack = midiFile.getTracks().get(i).getEvents();
            for (MidiEvent midiEvent : eventsOfTrack) {
                String eventContent = new String(midiEvent.toString());
                if (eventContent.contains("(0): Text:")) {
                    continue;
                } else if (eventContent.contains("): Text:")) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static List<Long> createNotesToFind(MidiFile midiFile){
        List<Long> notesToFind = new ArrayList<Long>();
        TreeSet<MidiEvent> vocalTrackEvents = midiFile.getTracks().get(getTextTrack(midiFile)).getEvents();
        for (MidiEvent midiEvent: vocalTrackEvents){
            int noteValue;
            String str = midiEvent.toString();
            String[] arr = str.split(" ");
            noteValue = Integer.parseInt(arr[0]);
            if (noteValue != 0){
                notesToFind.add((long) noteValue);
            }
        }
        return notesToFind;
    }

    public static List<Note> getVocalNotes(MidiFile midiFile){
        List<Long> toFind = createNotesToFind(midiFile);
        for (int i = 0;i < midiFile.getTrackCount();i++){
            List<Long> test = eventsToNotesCopy(midiFile.getTracks().get(i).getEvents());
            if (toFind.equals(test)){
                return eventsToNotes(midiFile.getTracks().get(i).getEvents());
            }
        }
        return null;
    }

    public static void analyzeRange(List<Note> notes){
        int min = notes.get(0).sign().getMidi();
        int max = notes.get(0).sign().getMidi();
        int minIndex = -1;
        int maxIndex = -1;

        for (int i = 0;i < notes.size();i++){
            int test = notes.get(i).sign().getMidi();
            if (test >= max){
                max = test;
                maxIndex = i;
            }
            if (test <= min){
                min = test;
                minIndex = i;
            }
        }
        System.out.println("Top note: " + notes.get(maxIndex).sign().fullName());
        System.out.println("Bottom note: " + notes.get(minIndex).sign().fullName());
        System.out.println("Range: " + (max - min));
    }

    public static void main(String[] args) throws IOException {
        MidiFile midiFile = new MidiFile(new FileInputStream("C:\\Users\\User\\liga-internship\\javacore-song-task\\src\\main\\resources\\Underneath Your Clothes.mid"));
        List<Note> notes = getVocalNotes(midiFile);
        analyzeRange(notes);
    }

    /**
     * Этот метод, чтобы вы не афигели переводить эвенты в ноты
     *
//     * @param events эвенты одного трека
     * @return список нот
     */
    public static List<Note> eventsToNotes(TreeSet<MidiEvent> events) {
        List<Note> vbNotes = new ArrayList<>();

        Queue<NoteOn> noteOnQueue = new LinkedBlockingQueue<>();
        for (MidiEvent event : events) {
            if (event instanceof NoteOn || event instanceof NoteOff) {
                if (isEndMarkerNote(event)) {
                    NoteSign noteSign = NoteSign.fromMidiNumber(extractNoteValue(event));
                    if (noteSign != NoteSign.NULL_VALUE) {
                        NoteOn noteOn = noteOnQueue.poll();
                        if (noteOn != null) {
                            long start = noteOn.getTick();
                            long end = event.getTick();
                            vbNotes.add(
                                    new Note(noteSign, start, end - start));
                        }
                    }
                } else {
                    noteOnQueue.offer((NoteOn) event);
                }
            }
        }
        return vbNotes;
    }

    public static List<Long> eventsToNotesCopy(TreeSet<MidiEvent> events) {
        List<Long> vbNotes = new ArrayList<>();

        Queue<NoteOn> noteOnQueue = new LinkedBlockingQueue<>();
        for (MidiEvent event : events) {
            if (event instanceof NoteOn || event instanceof NoteOff) {
                if (isEndMarkerNote(event)) {
                    NoteSign noteSign = NoteSign.fromMidiNumber(extractNoteValue(event));
                    if (noteSign != NoteSign.NULL_VALUE) {
                        NoteOn noteOn = noteOnQueue.poll();
                        if (noteOn != null) {
                            long start = noteOn.getTick();
                            long end = event.getTick();
                            vbNotes.add(start);
                        }
                    }
                } else {
                    noteOnQueue.offer((NoteOn) event);
                }
            }
        }
        return vbNotes;
    }

    private static Integer extractNoteValue(MidiEvent event) {
        if (event instanceof NoteOff) {
            return ((NoteOff) event).getNoteValue();
        } else if (event instanceof NoteOn) {
            return ((NoteOn) event).getNoteValue();
        } else {
            return null;
        }
    }

    private static boolean isEndMarkerNote(MidiEvent event) {
        if (event instanceof NoteOff) {
            return true;
        } else if (event instanceof NoteOn) {
            return ((NoteOn) event).getVelocity() == 0;
        } else {
            return false;
        }

    }
}
