import com.dylibso.chicory.generated.Basic;
import com.dylibso.chicory.generated.Sum;

public class Example {
    public static void main(String[] args) {

        var basic = new Basic();
        System.out.println(basic.run());

        var sum = new Sum();
        System.out.println(sum.add(1, 2));
    }
}
