package org.example.command;
/**
 * הממשק Command מייצג פעולה כללית במערכת.
 * כל מחלקה שמממשת את הממשק הזה חייבת לדעת:
 * 1. לבצע את הפעולה שלה בעזרת execute.
 * 2. לבטל את הפעולה שלה בעזרת undo.
 *
 * זה חלק מתבנית העיצוב Command Pattern.
 */
public interface Command {
    // מתודות שכל פעולה חייבת לממש כדי לדעת איך לבצע את עצמה
    void execute();
    void undo();
}