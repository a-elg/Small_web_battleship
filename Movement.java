import java.io.Serializable;

public class Movement implements Serializable{
    int Type;
    int x;
    int y;
    Movement(int x,int y,int Type){
        this.x=x;
        this.y=y;
        this.Type=Type;
    }
}
