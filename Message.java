import java.io.Serializable;

public class Message implements Serializable{
    int Type;
        /**
         * 0-Start game
         * 1-Movement
         * 2-Waiting for other player
         */
    int x;//If Type=0 or 2, x means who starts/won the game 0=Server, 1=Client
    int y;
    Message(int x,int y,int Type){
        this.x=x;
        this.y=y;
        this.Type=Type;
    }
}
