package com.ruoyi.web.controller.fire;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FireSmsQuery;
import com.ruoyi.system.service.FireSmsReadService;
import com.ruoyi.system.fire.sms.FireSmsService;
@RestController
@RequestMapping("/fire/sms")
public class FireSmsController extends BaseController {
    private final FireSmsService service; private final FireSmsReadService reads;
    public FireSmsController(FireSmsService service, FireSmsReadService reads) { this.service = service; this.reads = reads; }
    @PreAuthorize("@ss.hasPermi('fire:sms:send')") @GetMapping("/recipient")
    public AjaxResult recipient(@RequestParam String type, @RequestParam long id, @RequestParam(required=false) Long parentId) { return success(service.recipient(type, id, parentId)); }
    @PreAuthorize("@ss.hasPermi('fire:sms:send')") @PostMapping("/send")
    public AjaxResult send(@RequestBody Map<String,Object> input) throws Exception {
        try { return success(service.send(input)); }
        catch (ServiceException rejected) {
            // These are pre-dispatch validation/scope failures. Transport/DB failures still propagate
            // so the UI treats a potentially dispatched request as uncertain rather than retryable.
            return success(Map.of("taskStatus", "rejected", "resultText", rejected.getMessage()));
        }
    }
    @PreAuthorize("@ss.hasPermi('fire:sms:send')") @GetMapping("/requests/{requestId}")
    public AjaxResult request(@PathVariable String requestId) { return success(service.request(requestId)); }
    @PreAuthorize("@ss.hasPermi('fire:sms:list')") @GetMapping("/list")
    public TableDataInfo list(FireSmsQuery query) { startPage(); return getDataTable(reads.records(query)); }
}
