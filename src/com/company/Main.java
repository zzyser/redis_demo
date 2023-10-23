package com.company;

import java.util.Random;
import java.util.Scanner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

public class Main {
    public static String userPhone;
    public static Scanner scanner;
    static Jedis jedis;

    public static void main(String[] args) {

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(50);
        config.setMaxIdle(10);
        JedisPool pool = new JedisPool(config, "localhost", 6379);
        jedis = pool.getResource();


        System.out.println("请输入手机号：");
        scanner = new Scanner(System.in);
        userPhone = scanner.nextLine();
        String key = userPhone + "VC";

        String code = CreateCode();
        jedis.set(key, code);
        jedis.expire(key, 120);
        System.out.println("请输入验证码：");
        String userCode = scanner.nextLine();
        if
        (userCode.equals(jedis.get(key))) {
            System.out.println("登陆成功！");
            MainMenu();

        } else
            System.out.println("验证码不正确！");

        jedis.close();
    }

    public static void MainMenu() {

        int choice = 9;

        while (true) {
            System.out.println("********1：抢劵***********");
            System.out.println("********2：抢红包***********");
            System.out.println("********3：消费********");
            System.out.println("********4：用户充值********");
            System.out.println("********5：用户余额********");
            System.out.println("********9：退出***********");
            Scanner scanner = new Scanner(System.in);
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    coupon();
                    break;
                case 2:
                    cash();
                    break;
                case 3:
                    consumption();
                    break;
                case 4:
                    recharge();
                    break;
                case 5:
                    balance();
                    break;
                case 9:
                    return;
            }
        }
    }

    public static void coupon() {
        System.out.println("请输入秒杀商品的ID：");
        scanner = new Scanner(System.in);
        String goodsID = scanner.nextLine();

        String ticketKey = goodsID + "Stock";
        String BuyersKey = goodsID + "SUser";


        String kc = jedis.get(ticketKey);

        if (kc == null) {
            System.out.println("该商品劵不存在(눈_눈)");
        } else if (Integer.parseInt(kc) <= 0) {
            System.out.println("该商品劵已被抢光(눈_눈)");

        } else if (jedis.sismember(BuyersKey, userPhone)) {
            System.out.println("您已秒杀该商品");
        } else {

            //函数功能
            jedis.watch(ticketKey);
            Transaction multi = jedis.multi();
            multi.decr(ticketKey);
            multi.sadd(BuyersKey, userPhone);
            if (multi.exec() == null) {
                System.out.println("写入数据失败┑(￣Д ￣)┍ ");
            } else {
                System.out.println("抢劵成功");
            }

        }
        //
    }

    public static void recharge() {
        while (true) {
            System.out.println("请输入充值金额：");
            scanner = new Scanner(System.in);
            double number;
            try {
                number = scanner.nextDouble();
            } catch (Exception e) {
                System.out.println("请输入合法的金额数字！ ┑(￣Д ￣)┍  ");
                continue;
            }
            if (number > 0) {
                String strKey = userPhone + "M";
                String currentAmount = jedis.get(strKey);
                if (currentAmount != null) {
                    number = number + Double.parseDouble(currentAmount);
                }
                jedis.set(strKey, String.valueOf(number));
                System.out.println("充值成功\t当前余额为：" + number);
                break;
            } else {
                System.out.println("金额不能<=0(눈_눈)");
            }
        }
    }

    public static void balance() {
        double num = Double.parseDouble(jedis.get(userPhone + "M"));
        System.out.println("当前余额为：" + num);
    }

    public static void cash() {
        System.out.println("请输入红包口令");
        scanner = new Scanner(System.in);
        String CashID = scanner.nextLine();
        String ticketKey = CashID + "Cash";
        String buyersKey = CashID + "CUser";

        String kc = jedis.get(ticketKey);

        if (kc == null) {
            System.out.println("该红包不存在(눈_눈)");
        } else if (Integer.parseInt(kc) <= 0) {
            System.out.println("该红包已被抢光(눈_눈)");
        } else if (jedis.sismember(buyersKey, userPhone)) {
            System.out.println("您已抢过该红包");
        } else {
            jedis.watch(ticketKey);
            Transaction multi = jedis.multi();
            multi.decr(ticketKey);
            multi.sadd(buyersKey, userPhone);
            if (multi.exec() == null) {
                System.out.println("写入数据失败┑(￣Д ￣)┍ ");
            } else {
                System.out.println("抢红包成功");
            }
        }
    }

    public static void consumption() {
        double money, price;
        int goodId=0;
        String userMoney = userPhone + "M";
        int count = 0;
        double cash = 20.0;
        double off = 0.8;
        System.out.println("请输入购买商品的ID：");
        scanner = new Scanner(System.in);
        try {
            goodId = scanner.nextInt();
        } catch (Exception e) {
            System.out.println("请输入合法的商品ID！ ┑(￣Д ￣)┍  ");
            return;
        }
        String keyCheck = String.valueOf(goodId);
        if (jedis.sismember(keyCheck + "SUser", userPhone)) {
            count += 1;
        }
        if (jedis.sismember(keyCheck + "CUser", userPhone)) {
            count += 2;
        }
        String priceStr = jedis.get(String.valueOf(goodId));
        //

        System.out.println("商品价格为："+priceStr);
        System.out.println("输入 1 确认购买");
        scanner=new Scanner(System.in);
        String I= scanner.nextLine();
        if(!I.equals("1")){
            System.out.println("取消购买");
            return;
        }
        //
        price = Double.parseDouble(priceStr);
        money = Double.parseDouble(jedis.get(userMoney));
        switch (count) {
            case 0 -> {
                System.out.println("未拥有优惠劵及现金红包");
                if (price <= money) {
                    String num = String.valueOf((money - price));
                    jedis.set(userMoney, num);
                    System.out.println("购买成功！\t 当前余额为"+num);

                } else {
                    System.out.println("余额不足 ┑(￣Д ￣)┍ ");
                }
            }
            case 1 -> {
                System.out.println("拥有优惠劵，但未拥有现金红包");
                price = off * price;
                if (price <= money) {
                    String num = String.valueOf((money - price));
                    jedis.set(userMoney, num);
                    //删优惠劵
                    jedis.srem(keyCheck + "SUser",userPhone);
                    System.out.println("购买成功！\t 当前余额为"+num);
                } else {
                    System.out.println("余额不足 ┑(￣Д ￣)┍ ");
                }
            }
            case 2 -> {
                System.out.println("拥有现金红包，但未拥有优惠劵");
                if (price <= (money + cash)) {
                    String num = String.valueOf((money - (price - cash)));
                    jedis.set(userMoney, num);
                    //删红包
                    jedis.srem(keyCheck + "CUser",userPhone);
                    System.out.println("购买成功！\t 当前余额为"+num);
                } else {
                    System.out.println("余额不足 ┑(￣Д ￣)┍ ");
                }
            }
            case 3 -> {
                System.out.println("拥有优惠劵及现金红包");
                price = off * price;
                if (price <= (money + cash)) {
                    String num = String.valueOf((money - (price - cash)));
                    jedis.set(userMoney, num);
                    //删优惠劵和红包
                    jedis.srem(keyCheck + "SUser",userPhone);
                    jedis.srem(keyCheck + "CUser",userPhone);
                    System.out.println("购买成功！\t 当前余额为"+num);
                } else {
                    System.out.println("余额不足 ┑(￣Д ￣)┍ ");
                }
            }
        }
    }

    public static String CreateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int randomDigit = random.nextInt(10);
            code.append(randomDigit);
        }
        return code.toString();
    }
}
