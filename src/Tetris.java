import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Tetris extends JFrame {
    public static void main(String[] args) {
        new Tetris().go();
    }

    private final static String TITLE = "Tetris";
    private final static int BLOCK_SIZE = 50;
    private final static int WIDTH_IN_BLOCKS = 10;
    private final static int HEIGHT_IN_BLOCKS = 18;
    final int FIELD_DX = 17; // determined experimentally
    final int FIELD_DY = 40;

    private final static int HELP_PANEL_WIDTH = 155;

    private final static int KEY_LEFT = 37;
    private final static int KEY_UP = 38;
    private final static int KEY_RIGHT = 39;
    private final static int KEY_DOWN = 40;
    private final static int KEY_SPACE = 32;
    private int speed = 400;

    private int[][] filledBlocks = new int[HEIGHT_IN_BLOCKS][WIDTH_IN_BLOCKS];
    private int[][][] figuerList = new int[][][]{
            {{0,0,0,0}, {1,1,1,1}, {4, 0x00F0F0}}, // I
            {{1,0,0,0}, {1,1,1,0}, {3, 0x0000F0}}, // J
            {{0,0,1,0}, {1,1,1,0}, {3, 0xF0A000}}, // L
            {{1,1,0,0}, {1,1,0,0}, {2, 0xF0F000}}, // O
            {{0,1,1,0}, {1,1,0,0}, {3, 0x00F000}}, // S
            {{1,1,0,0}, {0,1,1,0}, {3, 0xF00000}}, // Z
            {{0,1,0,0}, {1,1,1,0}, {3, 0xA000F0}}  // T
    };
    private int[][][] figureRotateRule = new int[][][] {
            {{0,1}, {3,0}, {-2,-3}, {-1,2}},
            {{1,0}, {2,-1}, {-3,-2}, {0,3}}
    };

    private boolean pause = false;
    private boolean gameOver = false;
    private int nextFigureIndex = new Random().nextInt(figuerList.length);
    private Field field = new Field();
    private Figure figure = new Figure();
    private int score = 0;
    private int[] scoreList = new int[]{10, 25, 60, 145};

    public Tetris() {
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(BLOCK_SIZE * WIDTH_IN_BLOCKS + HELP_PANEL_WIDTH + FIELD_DX, BLOCK_SIZE * HEIGHT_IN_BLOCKS + FIELD_DY);
        field.setSize(BLOCK_SIZE * WIDTH_IN_BLOCKS + FIELD_DX, BLOCK_SIZE * HEIGHT_IN_BLOCKS + FIELD_DY);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width / 2) - (getWidth() / 2), (d.height / 2) - (getHeight() / 2));
        setIconImage(getTetrisImageIcon());
        setResizable(false);
        setVisible(true);
        setLayout(new BorderLayout());
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KEY_SPACE) {
                    pause = !pause;
                    field.repaint();
                }
                if (pause)
                    return;
                switch (e.getKeyCode()) {
                    case KEY_UP:
                        figure.rotate();
                        break;
                    case KEY_DOWN:
                        figure.dropDown();
                        break;
                    case KEY_LEFT:
                        figure.moveLeft();
                        break;
                    case KEY_RIGHT:
                        figure.moveRight();
                        break;
                }
            }
        });
        nextFigureIndex = new Random().nextInt(figuerList.length);
        add(field);
    }
    private void go() {
        while (!gameOver){
            try {
                Thread.sleep(speed);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (pause)
                continue;
            if (figure.isTouchGround()) {
                figure.liveOnTheGroung();
                checkFilling();
                figure = new Figure();
                nextFigureIndex = new Random().nextInt(figuerList.length);
                gameOver = !figure.isValidBlockListPoss(figure.blockList);
                if (gameOver) {
                    field.repaint();
                    int res = JOptionPane.showOptionDialog(this,
                            "Congratulations !!! Your score is " + score + ". Do you want to restart game?",
                            "Game Over",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, null, null);
                    if (res == JOptionPane.YES_OPTION)
                    {
                        for (int[] line : filledBlocks)
                            Arrays.fill(line, 0);
                        score = 0;
                        speed = 400;
                        gameOver = false;
                        go();
                    }
                }
            }
            else {
                figure.stepDown();
            }
            field.repaint();
        }
    }
    private void checkFilling() {
        int filledCount = 0;
        int i = filledBlocks.length - 1;
        while (i > 0) {
            boolean isFilled = true;
            for (int j : filledBlocks[i]) {
                if (j <= 0)
                    isFilled = false;
            }
            if (isFilled) {
                filledCount++;
                for (int k = i; k > 0; k--) {
                    System.arraycopy(filledBlocks[k - 1], 0, filledBlocks[k], 0, filledBlocks[k].length);
                }
            } else {
                i--;
            }
        }
        if (filledCount > 0) {
            score += scoreList[filledCount - 1];
            speed -= speed * 0.01 * filledCount; // - 1% for each line
        }
    }
    private Image getTetrisImageIcon() {
        Image icon = null;
        try {
            icon = new ImageIcon(new URL("http://icons.iconarchive.com/icons/chrisbanks2/cold-fusion-hd/128/tetris-icon.png")).getImage();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return icon;
    }

    class Figure {
        public ArrayList<Block> blockList = new ArrayList<Block>();
        private int[][] figureSettings;
        private int color;
        private int size;
        private int figurePos = 0; // →(0) ↑(1) ←(2) ↓(3)
        public Figure() {
            figureSettings = figuerList[nextFigureIndex];
            size = figureSettings[2][0];
            color = figureSettings[2][1];
            int startPosX = (WIDTH_IN_BLOCKS / 2) - (size/ 2);

            for (int line = 0; line < 2; line++) {
                for (int i = 0; i < figureSettings[line].length; i++) {
                    if (figureSettings[line][i] == 1) {
                        blockList.add(new Block(startPosX + i, (line - (size == 4 ? 1 : 0)), "" + i + line));
                    }
                }
            }
        }

        public void paint(Graphics g) {
            for (Block b : blockList) {
                b.paint(g, color);
            }
        }
        public void rotate() {
            if (size == 2) //square
                return;

            ArrayList<Block> tmpBlockList =  new ArrayList<Block>();
            for (Block b : blockList)
                    tmpBlockList.add(b.clone());

            rotateBlockList(tmpBlockList);

            if(isValidBlockListPoss(tmpBlockList)) {
                this.blockList = tmpBlockList;
                figurePos = figurePos == 3 ? 0 : figurePos + 1;
                field.repaint();
            }
        }

        private boolean isValidBlockListPoss(ArrayList<Block> blockList) {
            for (Block b : blockList) {
                if(b.getY() < 0)
                {
                    // may be true when we rotate figure at the top and other bloks are valid;
                }
                else if (b.getX() < 0 || b.getX() >= WIDTH_IN_BLOCKS || b.getY() >= HEIGHT_IN_BLOCKS || filledBlocks[b.getY()][b.getX()] > 0)
                   return false;
            }
            return true;
        }

        private ArrayList<Block> rotateBlockList(ArrayList<Block> blockList) {
            for (int line = 0; line < 2; line++) { // 2 lines of figure
                for (int i = 0; i < figureSettings[line].length; i++) { // rotat of each line
                    if (figureSettings[line][i] == 1) {
                        for (Block bl : blockList) {
                            if (bl.getKey().equals("" + i + line)) {

                                //scale
                                if(figurePos == 1) {
                                    bl.setX(bl.getX() - (4 - size));
                                    if(size == 4)
                                        bl.setY(bl.getY() + 1);
                                }
                                if(figurePos == 2) {
                                    bl.setX(bl.getX() + (4 - size));
                                    bl.setY(bl.getY() + (4 - size));
                                    if(size == 4) {
                                        bl.setX(bl.getX() + 1);
                                        bl.setY(bl.getY() - 1);
                                    }
                                }
                                if (figurePos == 3) {
                                    bl.setY(bl.getY() - (4 - size));
                                    if(size == 4)
                                        bl.setX(bl.getX() - 1);
                                }
                                bl.setX(bl.getX() + (figureRotateRule[line][figurePos][0] - (i * (figurePos == 3 || figurePos == 2 ? -1 : 1))));
                                bl.setY(bl.getY() + (figureRotateRule[line][figurePos][1] - (i * (figurePos == 1 || figurePos == 2 ? -1 : 1))));
                            }
                        }
                    }
                }
            }
            return blockList;
        }

        public void dropDown() {
            while (!isTouchGround()) {
                stepDown();
            }
        }

        private boolean isTouchGround() {
            for (Block b : blockList) {
                if (b.getY() == HEIGHT_IN_BLOCKS - 1 || (b.getY() + 1 >= 0 && filledBlocks[b.getY() + 1][b.getX()] > 0)) {
                    return true;
                }
            }
            return false;
        }

        public void stepDown() {
            for (Block b : blockList) {
                b.setY(b.getY() + 1);
            }
        }

        public void moveLeft() {
            for (Block b : blockList) {
                if (b.getX() == 0 || (b.getY() >= 0 && filledBlocks[b.getY()][b.getX() - 1] > 0))
                {
                    return;
                }
            }
            for (Block b : blockList) {
                b.setX(b.getX() - 1);
            }
            field.repaint();
        }

        public void moveRight() {
            for (Block b : blockList) {
                if (b.getX() == WIDTH_IN_BLOCKS - 1 || (b.getY() >= 0 && filledBlocks[b.getY()][b.getX() + 1] > 0))
                {
                    return;
                }
            }
            for (Block b : blockList) {
                b.setX(b.getX() + 1);
            }
            field.repaint();
        }

        public void liveOnTheGroung() {
            for (Block b : blockList) {
                filledBlocks[b.getY()][b.getX()] = color;
            }
        }
    }

    class Block {
        private int x, y;
        private String key;

        Block(int x, int y, String key) {
            setX(x);
            setY(y);
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public Block clone() {
            return new Block(getX(), getY(), key);
        }

        public void paint(Graphics g, int color) {
            g.setColor(new Color(color));
            g.fill3DRect(getX() * BLOCK_SIZE + 3, getY() * BLOCK_SIZE + 3, BLOCK_SIZE - 6, BLOCK_SIZE - 6, false);
        }
    }

    class Field extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);

            for (int i = 0; i < WIDTH_IN_BLOCKS ; i++) {
                for (int j = 0; j < HEIGHT_IN_BLOCKS; j++) {
                    g.setColor(Color.BLACK);
                    if(i != 0 && j != 0) {
                        g.drawLine(i * BLOCK_SIZE, j * BLOCK_SIZE - 5, i * BLOCK_SIZE, j * BLOCK_SIZE + 5);
                        g.drawLine(i * BLOCK_SIZE - 5, j * BLOCK_SIZE, i * BLOCK_SIZE + 5, j * BLOCK_SIZE);
                    }
                    if(j >= 0 && filledBlocks[j][i] > 0) {
                        g.setColor(new Color(filledBlocks[j][i]));
                        g.fill3DRect(i * BLOCK_SIZE + 1, j * BLOCK_SIZE + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2, true);
                    }
                }
            }
            figure.paint(g);

            //draw HelpPanel
            Container parent = getParent();
            Point hpPoint = new Point(parent.getWidth() - HELP_PANEL_WIDTH, 0);

            //bg
            g.setColor(Color.white);
            g.fillRect(parent.getWidth() - HELP_PANEL_WIDTH, 0, HELP_PANEL_WIDTH, getHeight());
            g.setColor(Color.gray);
            g.drawLine(parent.getWidth() - HELP_PANEL_WIDTH, 0, parent.getWidth() - HELP_PANEL_WIDTH, getHeight());

            //separate lines
            g.setColor(Color.gray);
            g.drawLine(hpPoint.x, 50, parent.getWidth(), 50);
            g.drawLine(hpPoint.x, 675, parent.getWidth(), 675);

            //score
            Font f = new Font("Comic Sans MS", 1, 20);
            g.setFont(f);
            g.setColor(Color.darkGray);
            g.drawString("Score:", hpPoint.x + 20, 35);
            g.setColor(Color.blue);
            g.drawString("" + score, hpPoint.x + 90, 35);

            //game over indicator
            if(gameOver)
            {
                g.setColor(new Color(150, 0, 0));
                g.drawString("Game Over", hpPoint.x + 20, 100);
            }

            //next figure
            g.setColor(Color.darkGray);
            g.drawString("Next:", hpPoint.x + 45, 200);
            int smallSize = 35;
            int sx = hpPoint.x + 10 + (( 4 - figuerList[nextFigureIndex][2][0]) * smallSize / 2);
            int sy = 350;
            int[][] nextFigureCord = figuerList[nextFigureIndex];
            g.setColor(new Color(figuerList[nextFigureIndex][2][1]));
            for (int line = 0; line < 2; line++) {
                for (int i = 0; i < nextFigureCord[line].length; i++) {
                    if (nextFigureCord[line][i] == 1) {
                        g.fill3DRect(sx + (smallSize * i),sy + (smallSize * line), smallSize - 2, smallSize - 2, false);
                    }
                }
            }

            //pause indicator
            g.setFont(f);
            if (pause) {
                g.setColor(new Color(0, 200, 0));
            }
            else {
                g.setColor(Color.lightGray);
            }
            g.drawString("Pause " + (pause ? "ON": "OFF"), hpPoint.x + 30, 650);

            //tips
            g.setColor(Color.darkGray);
            g.setFont(new Font("Comic Sans MS", 1, 15));

            g.drawImage(new ImageIcon(getClass().getResource("img/up.png")).getImage(), hpPoint.x + 27, 700, 30, 30, null);
            g.drawImage(new ImageIcon(getClass().getResource("img/down.png")).getImage(), hpPoint.x + 27, 740, 30, 30, null);
            g.drawImage(new ImageIcon(getClass().getResource("img/left.png")).getImage(), hpPoint.x + 10, 780, 30, 30, null);
            g.drawImage(new ImageIcon(getClass().getResource("img/right.png")).getImage(), hpPoint.x + 45, 780, 30, 30, null);
            g.drawImage(new ImageIcon(getClass().getResource("img/space.png")).getImage(), hpPoint.x + 12, 820, 60, 30, null);

            g.drawString("- rotate", hpPoint.x + 80, 720);
            g.drawString("- down", hpPoint.x + 80, 760);
            g.drawString("- move", hpPoint.x + 80, 800);
            g.drawString("- pause", hpPoint.x + 80, 840);
        }
    }
}