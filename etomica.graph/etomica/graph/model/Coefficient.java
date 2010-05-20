package etomica.graph.model;

public interface Coefficient {

  public void add(Coefficient value);

  public Coefficient copy();

  public int getDenominator();

  public int getNumerator();

  public int getSign();

  public void inc();

  public void multiply(Coefficient value);

  public void setDenominator(int value);

  public void setNumerator(int value);

  public void switchSign();
}