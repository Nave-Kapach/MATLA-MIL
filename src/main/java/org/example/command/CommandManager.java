package org.example.command;

import java.util.Stack;

public class CommandManager {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();

    public void executeCommand(Command c) {
        c.execute();
        undoStack.push(c);
        redoStack.clear(); // כשמבצעים פעולה חדשה, מאפסים את היסטוריית ה-Redo
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command c = undoStack.pop();
            c.undo();
            redoStack.push(c);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command c = redoStack.pop();
            c.execute();
            undoStack.push(c);
        }
    }
}