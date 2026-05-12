package org.example.command;
import java.util.Stack;
/**
 * המחלקה CommandManager אחראית לנהל פעולות שניתן לעשות להן Undo ו-Redo.
 * היא שומרת שתי מחסניות:
 * undoStack - פעולות שכבר בוצעו ואפשר לבטל.
 * redoStack - פעולות שבוטלו ואפשר לבצע שוב.
 */
public class CommandManager {
    //איתחול מחסניות
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();
    public void executeCommand(Command c) {// מתודה שמקבלת פעולה כלשהי מסוג Command ומבצעת אותה
        c.execute();
        undoStack.push(c);//שמירה במחסנית את הפעולה
        redoStack.clear();//לאחר פעולה חדשה מאפסים את ההיסטוריה של REDO
    }
    public void undo() {
        // מתודה שמבטלת את הפעולה האחרונה שבוצעה
        if (!undoStack.isEmpty()) {
            Command c = undoStack.pop();//הוצאת הפעולה
            c.undo();
            redoStack.push(c);//מחזיר למסחסנית REDO כי היא שוב פעולה שעשינו
        }
    }
    public void redo() {
        // מתודה שמבצעת מחדש פעולה שבוטלה קודם
        if (!redoStack.isEmpty()) {
            Command c = redoStack.pop();//הוצאת הפעולה
            c.execute();//שימוש בפעולה
            undoStack.push(c);//מחזיר למסחסנית UNDO כי היא שוב פעולה שעשינו
        }
    }
}