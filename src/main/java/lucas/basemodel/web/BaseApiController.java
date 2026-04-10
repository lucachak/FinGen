package lucas.basemodel.web;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BaseApiController { // <-- CORRIGIDO PARA 'BaseApiController'

    private final HashMap<String, Object> map = new HashMap<>();

    @GetMapping("/")
    public Map<String, Object> teste(){
        map.put("Item", "Random Item");
        return map;
    }

    @PostMapping("/")
    public Map<String, Object> testePost(@RequestBody Map<String, Object> map_new){
        map.putAll(map_new);
        return map;
    }

    @PutMapping("/{id}")
    public Map<String, Object> testePut(@PathVariable String id, @RequestBody Map<String, Object> map_new){
        map.put(id, map_new);
        return map;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> testeDelete(@PathVariable String id){
        map.remove(id);
        return map;
    }
}