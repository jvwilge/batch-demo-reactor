package net.jvw.batchdemo;

public class Product {

  private long id;
  private double price;
  private int stock;
  private String name;

  public Product() {
  }

  public Product(long id, double price, int stock, String name) {
    this.id = id;
    this.price = price;
    this.stock = stock;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public int getStock() {
    return stock;
  }

  public void setStock(int stock) {
    this.stock = stock;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return String.format("%s\n" +
        "\"%s\",%.2f,%d,\"%s\"", id, id, price, stock, name);
  }
}
