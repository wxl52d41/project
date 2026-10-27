package com.wxl52d41.apisign.sample.demo;

import com.wxl52d41.apisign.starter.annotation.ApiSign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各种入参形态下的验签演示
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestController
@RequestMapping("demo/form")
public class SignFormDemoController {

    private static final Logger log = LoggerFactory.getLogger(SignFormDemoController.class);

    /**
     * 参数模式：query 参数签名
     *
     * 三件套（timestamp / nonce / sign）与业务参数一起拼在 URL query 上
     */
    @GetMapping("query")
    @ApiSign
    public Map<String, Object> query(@RequestParam String name, @RequestParam Integer age) {
        return result("query", "name", name, "age", age);
    }

    /**
     * 参数模式：urlencoded 表单签名
     *
     * 三件套与业务参数一起作为表单字段提交
     */
    @PostMapping("urlencoded")
    @ApiSign
    public Map<String, Object> urlencoded(@RequestParam String name, @RequestParam Integer age) {
        return result("urlencoded", "name", name, "age", age);
    }

    /**
     * 路径参数同样参与验签，防止资源 ID 被篡改
     *
     * goodsId 从 URI 模板解析后并入签名参数集合
     */
    @GetMapping("path/{goodsId}")
    @ApiSign
    public Map<String, Object> pathAndQuery(@PathVariable String goodsId, @RequestParam String name) {
        return result("path+query", "goodsId", goodsId, "name", name);
    }

    /**
     * 多值参数取首个值参与签名
     *
     * tags=a&tags=b 加签时按 tags=a 计算
     */
    @GetMapping("multi-value")
    @ApiSign
    public Map<String, Object> multiValue(@RequestParam List<String> tags) {
        return result("multi-value", "tags", tags);
    }

    /**
     * PUT + JSON body 原文签名（默认模式）
     *
     * 三件套走请求头，sign 覆盖 body 原文
     */
    @PutMapping("json-put")
    @ApiSign
    public Map<String, Object> jsonPut(@RequestBody Map<String, Object> body) {
        return result("json-put", "body", body);
    }

    /**
     * 中文与特殊字符参数（参数模式）
     *
     * 签名计算用原始值，URL 传输时正常编码
     */
    @GetMapping("chinese")
    @ApiSign
    public Map<String, Object> chinese(@RequestParam String name, @RequestParam String remark) {
        return result("chinese", "name", name, "remark", remark);
    }

    private Map<String, Object> result(String mode, Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", mode);
        for (int i = 0; i < kvs.length; i += 2) {
            m.put(String.valueOf(kvs[i]), kvs[i + 1]);
        }
        return m;
    }
}
