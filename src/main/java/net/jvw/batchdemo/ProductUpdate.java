package net.jvw.batchdemo;

import java.util.StringJoiner;

public class ProductUpdate {

  private long id;
  private double priceChange;
  private int stockChange;

  public ProductUpdate() {
  }

  public ProductUpdate(long id, double priceChange, int stockChange) {
    this.id = id;
    this.priceChange = priceChange;
    this.stockChange = stockChange;
  }


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public double getPriceChange() {
    return priceChange;
  }

  public void setPriceChange(double priceChange) {
    this.priceChange = priceChange;
  }

  public int getStockChange() {
    return stockChange;
  }

  public void setStockChange(int stockChange) {
    this.stockChange = stockChange;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ProductUpdate.class.getSimpleName() + "[", "]")
        .add("id=" + id)
        .add("priceChange=" + priceChange)
        .add("stockChange=" + stockChange)
        .toString();
  }

}
