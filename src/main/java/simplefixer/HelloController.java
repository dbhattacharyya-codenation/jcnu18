package simplefixer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("/chal")
    public String dumbMethod() {
        return "Chal gaya bhai!!";
    }
}
