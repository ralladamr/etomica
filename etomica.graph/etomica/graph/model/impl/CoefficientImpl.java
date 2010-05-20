package etomica.graph.model.impl;

import etomica.graph.model.Coefficient;

public class CoefficientImpl implements Coefficient {

  private int numerator;
  private int denominator;
  private int sign;

  public CoefficientImpl(int value) {

    this(value, 1, 1);
  }

  public CoefficientImpl(int numerator, int denominator) {

    this(numerator, denominator, 1);
  }

  public CoefficientImpl(int numerator, int denominator, int sgn) {

    this.numerator = numerator;
    this.denominator = denominator;
    sign = sgn;
  }

  public void add(Coefficient value) {

    if (getDenominator() == value.getDenominator()) {
      numerator = sign * numerator + value.getSign() * value.getNumerator();
    }
    else {
      numerator = sign * numerator * value.getDenominator() + value.getSign() * value.getNumerator() * denominator;
      denominator = getDenominator() * value.getDenominator();
    }
    if (numerator < 0) {
      numerator = -numerator;
      switchSign();
    }
  }

  public Coefficient copy() {

    return new CoefficientImpl(numerator, denominator, sign);
  }

  public int getDenominator() {

    return denominator;
  }

  public int getNumerator() {

    return numerator;
  }

  public int getSign() {

    return sign;
  }

  public void inc() {

    numerator += denominator;
  }

  public void multiply(Coefficient value) {

    numerator = numerator * value.getNumerator();
    denominator = denominator * value.getDenominator();
    sign = sign * value.getSign();
  }

  public void setDenominator(int value) {

    denominator = value;
  }

  public void setNumerator(int value) {

    numerator = value;
  }

  public void switchSign() {

    sign = -sign;
  }

  @Override
  public String toString() {

    return (sign < 0 ? "-" : "") + numerator + (denominator == 1 ? "" : "/" + denominator);
  }
}