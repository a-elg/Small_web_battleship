import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.*;
import org.javatuples.Pair;

public class Server {
    private ServerSocket SS;
        private final int Port=8090;
    private Socket Client;

    private boolean[][] VirtualBoard;
    private int[][] UsedButtons;// 0: Empty,1: Used and Missed,2: Used and Hit
        private int remainingSquares;
        private int checkedSquares;
    private Pair<Integer,Integer>Bound1,Bound2;
    private int NextDirection;// -1: Need new direction, 0: Up,1: Down,2: Right,3: Left
    private int streak;

    private Random RN;
    
    final int h_buttons=10+1;
    final int v_buttons=10+1;
    
    public static void main(String[] args) {new Server().StartServer(5000);}

    private void StartServer(int Port) {
        try {
            SS=new ServerSocket(Port);
            System.out.println("Server On.");
            RN=new Random();
            WaitForConection();
            
        } catch (Exception e) {e.printStackTrace();}
    }

    private void WaitForConection() {
        try {
            while(true) {
                Client=SS.accept();
                ProcessRequest();
            }
            //System.out.println("Se acabo");
        }
        catch (Exception e) {e.printStackTrace();}
    }

    private void ProcessRequest() {
        try {
            ObjectInputStream dis=new ObjectInputStream(Client.getInputStream());
            Message message=(Message)dis.readObject();
            switch (message.Type) {
                case 0:// Initialize
                    SetNewGame();
                    break;
                case 1://Recieve a shot
                    GiveFeedback(message.y,message.x);
                    if((remainingSquares<=0)||(checkedSquares>=99)) 
                        break;
                    Load();//Shot back

                    break;

                case 2://Make first shot
                    Load();
                    break;

                default:
                    System.out.println("This is not supposed to happen b.b");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SetNewGame() {
        //Generate board
        streak=0;
        VirtualBoard=new boolean[v_buttons][h_buttons];
        UsedButtons=new int[v_buttons][h_buttons];
        for(int i=0;i!=v_buttons;i++)
            for(int j=0;j!=h_buttons;j++){
                VirtualBoard[i][j]=false;
                UsedButtons[i][j]=0;
            }
        SetShips();
        PrintBoard();
    }

    //Print VirtualBoard
    private void PrintBoard() {
        System.out.println("     x    ");
        System.out.println("");
        System.out.print("   ");
        for(int i=0;i!=11;i++){
            System.out.print(Integer.toHexString(i));
        }
        System.out.println("");
        for(int i=0;i!=v_buttons;i++){
            if(i==5)
                System.out.print("y");
            else
                System.out.print(" ");
            System.out.print(" "+Integer.toHexString(i));
            for(int j=0;j!=h_buttons;j++){
                if(VirtualBoard[i][j])
                    System.out.print("1");
                else
                    System.out.print("0");
            }
            System.out.println();
        }
    }

    private void SetShips() {
        System.out.println("Setting ships...");
        int ShipsDidSet=0;
        int squares=4;
            remainingSquares=21;
            NextDirection=-1;
            checkedSquares=0;
        while(ShipsDidSet<=7){
            switch (ShipsDidSet) {
                case 1:
                case 2:
                    squares=3;
                    break;
                
                case 3:
                case 4:
                case 5:
                    squares=2;
                    break;
                    
                case 6:
                    squares=5;
                    break;
            }
            ShipsDidSet+=BlockFromTo((Math.abs(RN.nextInt())%(h_buttons-1))+1,(Math.abs(RN.nextInt())%(v_buttons-1))+1, RN.nextBoolean(), squares);
        }
    }

    public int BlockFromTo(int y,int x,Boolean H_V,int squares){
        if(H_V){//Horizontal choose
            if(x+squares>h_buttons)
                return 0;
            
            // Makes sure that the squares are not already occupied
            for(int j=0;j!=squares;j++)
                if(VirtualBoard[y][x+j])
                    return 0;

            for(int j=0;j!=squares;j++)
                VirtualBoard[y][x+j]=true;

            return 1;
        }
        else{//Vertical Choose
            if(y+squares>v_buttons)
                return 0;
            
            for(int j=0;j!=squares;j++)
                if(VirtualBoard[y+j][x])
                    return 0;
                
            for(int j=0;j!=squares;j++)
                VirtualBoard[y+j][x]=true;
        
            return 1;
        }
    }

    private void Load(){
        int y,x;
        boolean Trying_to_shot=true;
        do{
            switch (NextDirection) {
                case -1:
                    do{
                        y=(Math.abs(RN.nextInt())%(v_buttons-1))+1;
                        x=(Math.abs(RN.nextInt())%(h_buttons-1))+1;
                    }while(UsedButtons[y][x]!=0);
                    if(Shot(y,x)){
                        NextDirection=0;
                        Bound1=new Pair<>(y,x);
                        Bound2=new Pair<>(y,x);
                        UsedButtons[y][x]=2;
                        streak=1;
                        return;
                    }
                    else{
                        UsedButtons[y][x]=1;
                        streak=0;
                    }

                    break;
                case 0:
                    if(Bound2.getValue0()+1<v_buttons){//Out of bound?
                        if(UsedButtons[Bound2.getValue0()+1][Bound2.getValue1()]!=0){//Already shot?
                            NextDirection=1;
                            break;
                        }

                        if(Shot(Bound2.getValue0()+1,Bound2.getValue1())){
                            NextDirection=0;
                            UsedButtons[Bound2.getValue0()+1][Bound2.getValue1()]=2;
                            Bound2=new Pair<>(Bound2.getValue0()+1,Bound2.getValue1());
                            streak++;
                        }
                        else{
                            NextDirection=1;
                            UsedButtons[Bound2.getValue0()+1][Bound2.getValue1()]=1;
                        }
                        Trying_to_shot=false;
                    }
                    else// Try down direction
                        NextDirection=1;
                    break;
                case 1:
                    if(Bound1.getValue0()-1>0){//Out of bound?
                        if(UsedButtons[Bound1.getValue0()-1][Bound1.getValue1()]!=0){//Already shot?
                            if(streak>1)//If streak is more than 1, try to shoot in the other square
                                NextDirection=-1;
                            else//If streak is 1, try to shoot in the other direction
                                NextDirection=2;
                            break;
                        }

                        if(Shot(Bound1.getValue0()-1,Bound1.getValue1())){//If hit
                            NextDirection=1;
                            UsedButtons[Bound1.getValue0()-1][Bound1.getValue1()]=2;
                            Bound1=new Pair<>(Bound1.getValue0()-1,Bound1.getValue1());
                            streak++;
                        }
                        else{//If miss
                            if(streak>1){//If streak is more than 1, try to shoot in the other square
                                NextDirection=-1;
                            }
                            else{//If streak is 1, try to shoot in the other direction
                                NextDirection=2;
                            }
                            UsedButtons[Bound1.getValue0()-1][Bound1.getValue1()]=1;
                        }
                        Trying_to_shot=false;
                    }
                    else{
                        if(streak>1)
                            NextDirection=-1;
                        else
                            NextDirection=2;
                    }   
                    break;
                case 2:
                    if(Bound2.getValue1()+1<h_buttons){//Out of bound?
                        if(UsedButtons[Bound2.getValue0()][Bound2.getValue1()+1]!=0){//Already shot?
                            NextDirection=3;
                            break;
                        }

                        if(Shot(Bound2.getValue0(),Bound2.getValue1()+1)){
                            NextDirection=2;
                            UsedButtons[Bound2.getValue0()][Bound2.getValue1()+1]=2;
                            Bound2=new Pair<>(Bound2.getValue0(),Bound2.getValue1()+1);
                            streak++;
                        }
                        else{
                            NextDirection=3;
                            UsedButtons[Bound2.getValue0()][Bound2.getValue1()+1]=1;
                        }
                        Trying_to_shot=false;
                    }
                    else{//Try right direction
                        NextDirection=3;
                    }
                    break;
                case 3:
                    if(Bound1.getValue1()-1>0){//Out of bound?
                        if(UsedButtons[Bound1.getValue0()][Bound1.getValue1()-1]!=0){//Already shot?
                            NextDirection=-1;
                            break;
                        }

                        if(Shot(Bound1.getValue0(),Bound1.getValue1()-1)){//If hit
                            NextDirection=3;
                            UsedButtons[Bound1.getValue0()][Bound1.getValue1()-1]=2;
                            Bound1=new Pair<>(Bound1.getValue0(),Bound1.getValue1()-1);
                            streak++;
                        }
                        else{//If miss
                            NextDirection=-1;
                            UsedButtons[Bound1.getValue0()][Bound1.getValue1()-1]=1;
                        }
                        Trying_to_shot=false;
                    }
                    else{
                        NextDirection=-1;
                    }
                    break;
            }
        }while(Trying_to_shot);
    }

    private boolean Shot(int y,int x){
        System.out.print("\nShoting... y:"+y+" x:"+x+" ");
        try {
            ObjectOutputStream dos=new ObjectOutputStream(Client.getOutputStream());
            dos.writeObject(new Message(y,x,1));
            ObjectInputStream dis=new ObjectInputStream(Client.getInputStream());
            Answer ans=(Answer)dis.readObject();
            if (ans.result){
                System.out.print("Hit");
                remainingSquares--;
            }
            else
                System.out.print("Miss");
            checkedSquares++;
            System.out.println("\nCS:"+checkedSquares+"~RS:"+remainingSquares);
            return ans.result;
        } catch (Exception e) {
            System.out.println("Failed to shot...");
            e.printStackTrace();
            System.exit(0);
            return false;
        }
    }

    private void GiveFeedback(int y,int x){
        try {
            ObjectOutputStream dos=new ObjectOutputStream(Client.getOutputStream());
            dos.writeObject(new Answer(VirtualBoard[y][x]));
        } catch (Exception e) {
            System.out.println("Failed to give feedback...");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void FirstShot(){
        int y=(Math.abs(RN.nextInt())%(v_buttons-1))+1;
        int x=(Math.abs(RN.nextInt())%(h_buttons-1))+1;
        try {
            ObjectOutputStream dos=new ObjectOutputStream(Client.getOutputStream());
            dos.writeObject(new Message(y,x,1));
            ObjectInputStream dis=new ObjectInputStream(Client.getInputStream());
            Answer ans=(Answer)dis.readObject();
            if (ans.result){
                System.out.print("Hit");
                remainingSquares--;
                NextDirection=0;
                Bound1=new Pair<>(y,x);
                Bound2=new Pair<>(y,x);
                UsedButtons[y][x]=2;
                streak=1;        
                
            }
            else{
                UsedButtons[y][x]=1;
                streak=0;
                System.out.print("Miss");
                NextDirection=-1;
            }
            checkedSquares++;

        } catch (Exception e) {
            System.out.println("Failed to give first shot...");
            e.printStackTrace();
            System.exit(0);
        }
    }
}
