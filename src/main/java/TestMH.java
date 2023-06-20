import com.viettel.security.PassTranformer;

public class TestMH {
    public static void main(String[] args) throws Exception {
        String pass = "ChanVai#1990";
        String MH = PassTranformer.encrypt(pass);
        String GM = PassTranformer.decrypt(MH);
        System.out.println(MH);
        System.out.println(GM);
    }
}
