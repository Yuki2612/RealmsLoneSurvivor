package gameproject;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import gameproject.state.PlayingState;

public class InputManager implements KeyListener, MouseListener, MouseMotionListener {
    public boolean isMouseHolding = false;
    public int mouseX = 0;
    public int mouseY = 0;
    public boolean mouseClicked = false;
    public boolean escPressed = false;
    public boolean rPressed = false;
    public boolean iPressed = false;
    public boolean showLargeMap = false;
    public boolean enterPressed = false;
    public String typedKeySequence = "";

    private GamePanel game;

    public InputManager(GamePanel game) {
        this.game = game;
    }

    public void clearClickAndKey() {
        mouseClicked = false;
        escPressed = false;
        rPressed = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) escPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_R) rPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_I) iPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_ENTER) enterPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_M) showLargeMap = !showLargeMap;
        
        if (game.player != null && game.getCurrentState() instanceof PlayingState) {
            game.player.keyPressed(e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) escPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_R) rPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_I) iPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_ENTER) enterPressed = false;
        if (game.player != null && game.getCurrentState() instanceof PlayingState) {
            game.player.keyReleased(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            isMouseHolding = true;
            mouseClicked = true;
            mouseX = e.getX();
            mouseY = e.getY();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            isMouseHolding = false;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override public void keyTyped(KeyEvent e) {
        typedKeySequence += e.getKeyChar();
        if (typedKeySequence.length() > 20) {
            typedKeySequence = typedKeySequence.substring(typedKeySequence.length() - 20);
        }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
