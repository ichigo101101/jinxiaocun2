package com.example.controller;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Goods;
import com.example.entity.Sale;
import com.example.entity.Stock;
import com.example.exception.CustomException;
import com.example.service.*;
import com.example.utils.ValidateCodeCache;
import com.example.utils.ValidateCodeProperties;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础前端接口
 */
@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    private StaffService staffService;
    @Resource
    private GoodsService goodsService;
    @Resource
    StockService stockService;
    @Resource
    SaleService saleService;




    @GetMapping("/")
    public Result hello() {
        return Result.success("访问成功");
    }

    @GetMapping("/getValidateCode")
    public void getValidateCode(HttpServletRequest request, HttpServletResponse response) {
        // 生成验证码
        ValidateCodeProperties code = new ValidateCodeProperties();
        setHeader(response, code.getType());
        Captcha captcha = createCaptcha(code);
        // 存储验证码到缓存
        ValidateCodeCache.setCache(request.getParameter("key"), captcha.text().toLowerCase());
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            captcha.out(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.VALIDATE_CODE_ERROR);
        }
    }
    /**
     * 创建验证码
     */
    private Captcha createCaptcha(ValidateCodeProperties code) {
        Captcha captcha = null;
        if ("gif".equalsIgnoreCase(code.getType())) {
            captcha = new GifCaptcha(code.getWidth(), code.getHeight(), code.getLength());
        } else {
            captcha = new SpecCaptcha(code.getWidth(), code.getHeight(), code.getLength());
        }
        captcha.setCharType(code.getCharType());

        return captcha;
    }
    /**
     * 设置验证码返回头
     */
    private void setHeader(HttpServletResponse response, String type) {
        if ("gif".equalsIgnoreCase(type)) {
            response.setContentType(MediaType.IMAGE_GIF_VALUE);
        } else {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
        }
        response.setHeader(HttpHeaders.PRAGMA, "No-cache");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0L);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())||ObjectUtil.isEmpty(account.getKey())
                ||ObjectUtil.isEmpty(account.getCode())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        //对验证码进行校验
        boolean result = ValidateCodeCache.validateCode(account.getKey(),account.getCode());
        if(!result){
            return Result.error(ResultCodeEnum.VALIDATE_CODE_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            account = adminService.login(account);
        }else if (RoleEnum.STAFF.name().equals(account.getRole())) {
            account = staffService.login(account);
        }

        //记录登录日志
        LogsService.recordLog("用户登录","登录",account.getUsername());
        return Result.success(account);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.STAFF.name().equals(account.getRole())) {
            staffService.register(account);
            //记录登录的日志
            LogsService.recordLog("用户注册","注册",account.getUsername());
        }
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getNewPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        }else if (RoleEnum.STAFF.name().equals(account.getRole())) {
            staffService.updatePassword(account);
        }
        return Result.success();
    }
    /**
     * 统计数据
     */
    @GetMapping("/dataCount")
    public Result dataCount() {
        List<Goods> goodsList = goodsService.selectAll(null);
        List<Stock> stockList = stockService.selectAll(null);
        List<Sale> saleList = saleService.selectAll(null);
        // 查询商品总库存
        Integer store = goodsList.stream().map(Goods::getNum).reduce(Integer::sum).orElse(0);
        // 查询商品的总金额
        Double money = goodsList.stream().map(goods -> goods.getPrice() * goods.getNum()).reduce(Double::sum).orElse(0D);
        // 查询进货数量
        Integer stock = stockList.stream().map(Stock::getNum).reduce(Integer::sum).orElse(0);
        // 查询销售数量
        Integer sale = saleList.stream().map(Sale::getNum).reduce(Integer::sum).orElse(0);
        Dict dict = Dict.create().set("store", store).set("money", money).set("stock", stock).set("sale", sale);
        return Result.success(dict);
    }
    /**
     * 统计过去一周的商品销售额
     */
    @GetMapping("/goodsCount")
    public Result goodsCount() {
        List<Sale> saleList = saleService.selectAll(null);
        // 获取最新的一周的数据
        Date today = new Date();
        // 从8天前到昨天  一周的时间集合
        List<DateTime> dateRange = DateUtil.rangeToList(DateUtil.offsetDay(today, -8), DateUtil.offsetDay(today, -1), DateField.DAY_OF_YEAR);
        // 排序
        dateRange = dateRange.stream().sorted((a, b) -> (int) (a.getTime() - b.getTime())).collect(Collectors.toList());
        List<Dict> list = new ArrayList<>();
        for (DateTime dateTime : dateRange) {
            String dateStr = DateUtil.formatDate(dateTime);// 把  datetime类型的数据转换成字符串
            Double sum = saleList.stream().filter(sale -> sale.getTime() != null &&sale.getTime().contains(dateStr)).map(Sale::getTotal).reduce(Double::sum).orElse(0D);
            Dict dict = Dict.create();
            dict.set("name", dateStr).set("value", sum);
            list.add(dict);
        }
//        for (DateTime dateTime : dateRange) {
//            String dateStr = DateUtil.formatDate(dateTime); // 把 datetime 类型的数据转换成字符串
//            Double sum = saleList.stream()
//                    .filter(sale -> {
//                        String time = sale.getTime();
//                        return time != null && time.contains(dateStr); // 添加空值检查
//                    })
//                    .map(Sale::getTotal)
//                    .reduce(Double::sum)
//                    .orElse(0D);
//            Dict dict = Dict.create();
//            dict.set("name", dateStr).set("value", sum);
//            list.add(dict);
//        }

        return Result.success(list);
    }

    /**
     * 统计过去一周的商品销售额
     */
    @GetMapping("/storeCount")
    public Result storeCount() {
        List<Goods> goodsList = goodsService.selectAll(null);
        List<Dict> list = new ArrayList<>();
        List<String> nameList = goodsList.stream().map(Goods::getName).collect(Collectors.toList());
        for (String name : nameList) {
            Dict dict = Dict.create();
            Integer sum = goodsList.stream().filter(goods -> goods.getName().equals(name)).map(Goods::getNum).reduce(Integer::sum).orElse(0);
            dict.set("name", name).set("value", sum);
            list.add(dict);
        }
        return Result.success(list);
    }


}
