import java.net.Socket;
import java.util.*;
import javax.sound.sampled.Port;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.awt.*;
import java.awt.Font.*;
import java.awt.event.*;
import org.javatuples.Pair;

/*
    Cada jugador (usuario, PC) contará con dos tableros(matrices) de juego de 10x10 casillas, 
    uno reflejará su tablero donde colocará sus naves 
        Botes                                   =Cuadros
        1 acorazado[4 casillas de longitud]     =4
        2 cruceros[3 casillas de longitud]      =6
        3 destructores[2 casillas de longitud]  =6
        1 submarino[5 casillas de longitud])    =5
    __________________________________________________
        7                                       =21    
    ya sea en posición vertical u horizontal. Y el otro reflejará el tablero de tiro (donde 
    se mostrarán los aciertos/fallos de los tiros al tablero del jugador oponente).
*/


public class Client {
    public static void main(String[] args) {new Client().StartClient();}
    
    Socket MySocket;
        final String Address="127.0.0.1";
        final int Port=8132;
        ObjectInputStream ois;
        ObjectOutputStream oos;

    JFrame Myboard;
        int MyRemainingSquares;
        JFrame V_H_Options;
            JRadioButton OptionH;
            JRadioButton OptionV;
        int boatsSet;
        JButton[][] MyButtonMatrix;
        boolean Myturn; 
        int[][] MyState;// To know if a square has a ship, and if it has been used
            /*
             * 0=Button not used
             * 1=Alive
             * 2=Dead
             * 3=Missed
             */

    JFrame Enemyboard;
        int EnemyRemainingSquares;
        JButton[][] EnemyButtonMatrix;
        int[][] EnemyState;// To know if a square has a ship, and if it has been used
            /*
             * 0=Button not used
             * 1=Miss
             * 2=Hit
             */

    final int h_buttons=10+1;
    final int v_buttons=10+1;
    final String [] Blues={"#4996FC","#5549FC","#7bb3fc","#1b2afc","#4958FC"};
    final String [] Ships={"Cruises","Destructors","Submarine"};
    Random RN=new Random();//Random number generator
    Map<JButton,Pair<Integer,Integer>> MyButtonMap=new HashMap<>();
    Map<JButton,Pair<Integer,Integer>> EnemyButtonMap=new HashMap<>();

    public void StartClient() {
        SetEnemyBoard();
        SetMyBoard();
        SetMyShips();
    }

    public void SetMyBoard(){
        MyRemainingSquares=21;
        MyState=new int[v_buttons][h_buttons];
        int length=500;
		int height=500;
        Myboard=new JFrame("My territory");
        Myboard.setSize(length,height);

        MyButtonMatrix=new JButton[v_buttons][h_buttons];
        for(int i=0;i!=v_buttons;i++){
            for(int j=0;j!=h_buttons;j++){
                final int i_aux=i;
                final int j_aux=j;
                MyState[i][j]=0;
                MyButtonMatrix[i][j]=new JButton();
                MyButtonMatrix[i][j].setBackground(Color.decode(Blues[Math.abs(RN.nextInt())%Blues.length]));
                MyButtonMatrix[i][j].setFocusPainted(false);
                MyButtonMatrix[i][j].addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent av) {
                        int squares=4;
                        JButton b=(JButton)av.getSource();
                        switch (boatsSet) {
                            case 1:
                            case 2:
                                V_H_Options.setTitle("Cruises");
                                squares=3;
                                break;
                            
                            case 3:
                            case 4:
                            case 5:
                                V_H_Options.setTitle("Destructors");
                                squares=2;
                                break;
                                
                            case 6:
                                V_H_Options.setTitle("Submanrine");
                                squares=5;
                                break;
                        }
                        BlockFromTo(MyButtonMap.get(b).getValue0(), MyButtonMap.get(b).getValue1(),OptionH.isSelected(),squares);
                        if(boatsSet>=7){
                            V_H_Options.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            for(int i2=0;i2!=v_buttons;i2++)
                                for(int j2=0;j2!=h_buttons;j2++)
                                    MyButtonMatrix[i2][j2].setEnabled(false);
                            StartGame();
                        }
                    }
                });
                
                MyButtonMap.put(MyButtonMatrix[i][j],Pair.with(i,j));
                Myboard.add(MyButtonMatrix[i][j]);
            }
        }
        
        MyButtonMatrix[0][0].setBackground(new Color(255,255,255));
        MyButtonMatrix[0][0].setEnabled(false);

        for(int i=1;i!=h_buttons;i++){
            MyButtonMatrix[0][i].setText(String.valueOf(i));
            MyButtonMatrix[0][i].setFont(new Font("Serif",Font.PLAIN,9));
            MyButtonMatrix[0][i].setBackground(new Color(255,255,255));
            MyButtonMatrix[0][i].setEnabled(false);
        }

        for(int i=1;i!=11;i++){
            MyButtonMatrix[i][0].setText(""+(char)('A'+i-1));
            MyButtonMatrix[i][0].setFont(new Font("Serif",Font.PLAIN,9));
            MyButtonMatrix[i][0].setBackground(new Color(255,255,255));
            MyButtonMatrix[i][0].setEnabled(false);
        }
        
        Myboard.setLayout(new GridLayout(v_buttons,h_buttons));
        Myboard.setLocationRelativeTo(null);
        Myboard.setResizable(false);
		Myboard.setVisible(true);
        Myboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void SetEnemyBoard(){
        EnemyRemainingSquares=21;
        int length=500;
		int height=500;
        Enemyboard=new JFrame("Enemy territory");
        Enemyboard.setSize(length,height);
        EnemyState=new int[v_buttons][h_buttons];
        EnemyButtonMatrix=new JButton[v_buttons][h_buttons];
        for(int i=0;i!=v_buttons;i++){
            for(int j=0;j!=h_buttons;j++){
                final int i_aux=i;
                final int j_aux=j;
                EnemyButtonMatrix[i][j]=new JButton();
                EnemyButtonMatrix[i][j].setBackground(Color.decode(Blues[Math.abs(RN.nextInt())%Blues.length]));
                EnemyButtonMatrix[i][j].setFocusPainted(false);
                EnemyButtonMatrix[i][j].setEnabled(false);
                EnemyButtonMatrix[i][j].addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent av) {
                        JButton b=(JButton)av.getSource();
                        if(Shot(EnemyButtonMap.get(EnemyButtonMatrix[i_aux][j_aux]).getValue0(),EnemyButtonMap.get(EnemyButtonMatrix[i_aux][j_aux]).getValue1())){
                            EnemyRemainingSquares--;
                            b.setIcon(new ImageIcon("./Boom.png"));
                            b.setBackground(Color.decode("#70301e"));
                            EnemyState[i_aux][j_aux]=2;
                        }else{
                            Color C=b.getBackground();
                            b.setBackground(new Color(Math.abs(C.getRed()-100),Math.abs(C.getGreen()-100),Math.abs(C.getBlue()-100)));
                            EnemyState[i_aux][j_aux]=1;
                        }
                        System.out.println("Shot success");
                        DisableBoard();
                        GameEnded();
                        System.out.println("Waiting for shot back");
                        RecieveShot();
                        System.out.println("Shot recieved");
                        GameEnded();
                    }
                });
                EnemyButtonMap.put(EnemyButtonMatrix[i][j],Pair.with(i,j));
                Enemyboard.add(EnemyButtonMatrix[i][j]);
            }
        }
        
        EnemyButtonMatrix[0][0].setBackground(new Color(255,255,255));
        EnemyButtonMatrix[0][0].setEnabled(false);

        for(int i=1;i!=h_buttons;i++){
            EnemyButtonMatrix[0][i].setText(String.valueOf(i));
            EnemyButtonMatrix[0][i].setFont(new Font(" Serif",Font.PLAIN,9));
            EnemyButtonMatrix[0][i].setBackground(new Color(255,255,255));
            EnemyButtonMatrix[0][i].setEnabled(false);
        }

        for(int i=1;i!=11;i++){
            EnemyButtonMatrix[i][0].setText(""+(char)('A'+i-1));
            EnemyButtonMatrix[i][0].setFont(new Font("Serif",Font.PLAIN,9));
            EnemyButtonMatrix[i][0].setBackground(new Color(255,255,255));
            EnemyButtonMatrix[i][0].setEnabled(false);
        }
        
        Enemyboard.setLayout(new GridLayout(v_buttons,h_buttons));
        //System.out.println((ButtonMap.get(ButtonBoard[1][1])).getValue0()+" "+(ButtonMap.get(ButtonBoard[1][1])).getValue1()); 
        Enemyboard.setLocationRelativeTo(null);
        Enemyboard.setResizable(false);
		Enemyboard.setVisible(true);
        Enemyboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void StartGame() {
        try {
            MySocket=new Socket(Address,Port);
            oos=new ObjectOutputStream(MySocket.getOutputStream());
            Myturn=RN.nextBoolean();
            JOptionPane.showMessageDialog(new JFrame("Who starts"), (Myturn)?"You start":"Enemy starts");
            oos.writeObject(new Message(0,0,0));
            oos.flush();
            if(Myturn){
                System.out.println("My turn! Yay!");
                EnableBoard();
            }else{
                DisableBoard();
                RecieveShot();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean Shot(int v,int h){
        try {
            System.out.println("Shooting");
            MySocket=new Socket(Address,Port);
            oos=new ObjectOutputStream(MySocket.getOutputStream());
            oos.writeObject(new Message(v,h,1));
            oos.flush();
            ois=new ObjectInputStream(MySocket.getInputStream());
            Answer ans=(Answer)ois.readObject();
            return ans.result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void SetMyShips() {
        boatsSet=0;
        V_H_Options=new JFrame();
        V_H_Options.setSize(300,200);
        V_H_Options.setLayout(null);
        V_H_Options.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        //V_H_Options.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        V_H_Options.setResizable(false);
        V_H_Options.setLocationRelativeTo(null);
        
        OptionH=new JRadioButton("Horizontal"); OptionH.setSelected(true);
        OptionV=new JRadioButton("Vertical");
        OptionH.setBounds(50,90,100,30);
        OptionV.setBounds(150,90,100,30);
        
        ButtonGroup bg=new ButtonGroup();
            bg.add(OptionH);
            bg.add(OptionV);
        
        JTextArea Instructions=new JTextArea("Select whether you prefer, vertical or horizontal.");
           Instructions.setBounds(50,20,180,60);
           Instructions.setLineWrap(true);
           Instructions.setEnabled(false);
           Instructions.setBackground(new Color(0,0,0,0));
           Instructions.setFont(new Font(" Serif",Font.PLAIN,15));
           Instructions.setDisabledTextColor(new Color(0,0,0));      

           
        V_H_Options.add(Instructions);
        V_H_Options.add(OptionH);
        V_H_Options.add(OptionV);
        V_H_Options.setVisible(true);
                
        V_H_Options.setTitle("Battleship");
    }

    public void BlockFromTo(int y,int x,Boolean H_V,int squares){
        if(H_V){//Horizontal choose
            if(x+squares>h_buttons)
                return;

            // Makes sure that the squares are not already occupied
            for(int j=0;j!=squares;j++)
                if(!MyButtonMatrix[y][x+j].isEnabled())
                    return;
    
            for(int j=0;j!=squares;j++){
                MyButtonMatrix[y][x+j].setBackground(new Color(00+boatsSet*25,01+boatsSet*25,50+boatsSet*25));
                MyButtonMatrix[y][x+j].setEnabled(false);
                MyState[y][x+j]=1;
            }
        }
        else{//Vertical Choose
            if(y+squares>v_buttons)
                return;
            
            for(int j=0;j!=squares;j++)
                if(!MyButtonMatrix[y+j][x].isEnabled())
                    return;
                
            for(int j=0;j!=squares;j++){
                MyButtonMatrix[y+j][x].setBackground(new Color(00+boatsSet*25,01+boatsSet*25,50+boatsSet*25));
                MyButtonMatrix[y+j][x].setEnabled(false);
                MyState[y+j][x]=1;
            }
        }
        boatsSet++;
    }

    public void DisableBoard() {
        for(int i=0;i!=v_buttons;i++)
            for(int j=0;j!=h_buttons;j++)
                EnemyButtonMatrix[i][j].setEnabled(false);
    }
    
    public void EnableBoard() {
        for(int i=0;i!=v_buttons;i++)
            for(int j=0;j!=h_buttons;j++)
                if(EnemyState[i][j]==0)
                    EnemyButtonMatrix[i][j].setEnabled(true);
    }

    public boolean GameEnded() {
        if (EnemyRemainingSquares==0){
            JOptionPane.showMessageDialog(new JFrame("Game Over"), "You Won!");
            return true;
        }
        else if (MyRemainingSquares==0){
            JOptionPane.showMessageDialog(new JFrame("Game Over"), "You Lost!");
            return true;
        }
        return false;
    }

    public void RecieveShot() {
        try {
            MySocket=new Socket(Address,Port);
            oos=new ObjectOutputStream(MySocket.getOutputStream());
            oos.writeObject(new Message(0, 0, 2));
            oos.flush();
            ois=new ObjectInputStream(MySocket.getInputStream());
            Message M=(Message)ois.readObject();
            if(MyState[M.y][M.x]==1){
                MyState[M.y][M.x]=2;
                MyButtonMatrix[M.y][M.x].setBackground(Color.decode("#b50303"));
                MyRemainingSquares--;
                oos.writeObject(new Answer(true));
            }
            else{
                MyState[M.y][M.x]=3;
                Color C=MyButtonMatrix[M.y][M.x].getBackground();
                MyButtonMatrix[M.y][M.x].setBackground(new Color(Math.abs(C.getRed()-100),Math.abs(C.getGreen()-100),Math.abs(C.getBlue()-100)));
                oos.writeObject(new Answer(false));
            }
            oos.flush();
            EnableBoard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}