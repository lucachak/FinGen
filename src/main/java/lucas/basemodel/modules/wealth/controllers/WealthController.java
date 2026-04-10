package lucas.basemodel.modules.wealth.controllers;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import lucas.basemodel.modules.wealth.models.WealthSuggestion;
import lucas.basemodel.modules.wealth.repositories.WealthSuggestionRepository;
import lucas.basemodel.modules.wealth.services.WealthService;
import lucas.basemodel.modules.wealth.services.WealthSuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wealth")
@RequiredArgsConstructor
public class WealthController {

    private final WealthService wealthService;
    private final WealthSuggestionService suggestionService;
    private final WealthSuggestionRepository suggestionRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();

        // Ensure we have a fresh snapshot
        wealthService.updateAllValuations(user.getId());
        WealthSnapshot latest = wealthService.createSnapshot(user);
        
        // Generate new suggestions based on this snapshot
        List<WealthSuggestion> suggestions = suggestionService.generateSuggestions(user, latest);

        Map<String, Object> response = new HashMap<>();
        response.put("totalNetWorth", latest.getTotalNetWorth());
        response.put("breakdown", latest.getBreakdownJson());
        response.put("suggestions", suggestions);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<WealthSnapshot>> getHistory(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(wealthService.getHistory(user.getId()));
    }

    @PostMapping("/assets")
    public ResponseEntity<Asset> addAsset(@AuthenticationPrincipal User user, @RequestBody Asset asset) {
        if (user == null) return ResponseEntity.status(401).build();
        asset.setUser(user);
        return ResponseEntity.ok(wealthService.saveAsset(asset));
    }
}
