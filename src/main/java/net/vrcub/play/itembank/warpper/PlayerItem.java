package net.vrcub.play.itembank.warpper;


import java.math.BigDecimal;
import java.util.*;

public class PlayerItem {
    private final Map<String, Integer> items;

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public synchronized void setScore(BigDecimal score) {
        this.score = score;
    }

    private String lastLogin;
    private BigDecimal score;

    public BigDecimal getScore() {
        return score;
    }
    public PlayerItem() {
        this.items = Collections.synchronizedMap(new HashMap<>());
        //this.score = calculate();
    }

//    public double calculate() {
//        Collection<BankItem> bankItems = ItemBankReloaded.getInstance().getCacheManager().getBankItemCache().values();
//
//        this.score = bankItems.stream()
//                .mapToDouble(bankItem -> {
//                    double price = bankItem.getPrice();
//                    String mat = bankItem.getName();
//                    int amount = items.getOrDefault(mat, 0);
//                    return price * amount;
//                })
//                .sum();
//
//        return BigDecimal.valueOf(this.score).setScale(2, RoundingMode.HALF_UP).doubleValue();
//    }

//    public PlayerItem rebuild() {
//        this.score = calculate();
//        return this;
//    }

    public int getItemAmount(String mat) {
        synchronized (items) {
            return items.get(mat);
        }
    }

    public void setItem(String mat, int amount) {
        synchronized (items) {
            this.items.put(mat, amount);
        }
    }

    public void addItemAmount(String mat, int amount) {
        synchronized (items) {
            this.items.put(mat, items.getOrDefault(mat, 0) + amount);
        }
    }

    public Map<String, Integer> getItems() {
        return new HashMap<>(items);
    }
//    public double getScore() {
//        return Math.round(score * 100.0) / 100.0;
//    }

}