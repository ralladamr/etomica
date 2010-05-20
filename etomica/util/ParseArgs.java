package etomica.util;

import java.lang.reflect.Field;

/**
 * Class that handles parsing of commandline arguments.  Arguments are used to
 * assign fields in the parameterWrapper, much as is done in ReadParameters for
 * reading input files.
 * 
 * @author Andrew Schultz
 */
public class ParseArgs {

    public ParseArgs() {
    }
    
    public ParseArgs(ParameterBase parameterWrapper) {
        setParameterWrapper(parameterWrapper);
    }
    
    /**
     * Returns the parameter wrapper.
     */
    public ParameterBase getParameterWrapper() {
        return wrapper;
    }

    /**
     * Sets the parameterWrapper
     */
    public void setParameterWrapper(ParameterBase newParameterWrapper) {
        wrapper = newParameterWrapper;
        fields = wrapper.getClass().getFields();
    }

    /**
     * Parses each argument and attempts to match it with a field from
     * parameterWrapper and sets the field to the value from the next argument.
     * If the next argument is another option (or if there are no more
     * options), the value is taken to be 'true' with the hope that the field
     * is a boolean.
     * Options are accepted in the form "-numSteps" or "--numSteps".
     * This routine handles boolean, int, long, double, String.  Arrays are
     * handled if they are quoted.
     * 
     * -alpha "0.3 0.5"
     *  ==> alpha = {0.3, 0.5};
     */
    public void parseArgs(String[] args) {
        for (int i=0; i<args.length; i++) {
            if (args[i].charAt(0) != '-') {
                throw new RuntimeException("encountered "+args[i]+" when I was expecting an option");
            }
            String token = args[i].replaceAll("^--?", "");
            String value = "";
            if (i+1 == args.length || (args[i+1].charAt(0) == '-' && !Character.isDigit(args[i+1].charAt(1)))) {
                // last argument or next argument is another option.  hope this is a boolean parameter
                value = "true";
            }
            else {
                value = args[i+1];
                i++;
            }

            boolean foundField = false;
            for (int j=0; j<fields.length; j++) {
                if (token.equals(fields[j].getName())) {
                    wrapper.setValue(fields[j],value);
                    foundField = true;
                    break;
                }
            }
            if (!foundField) {
                throw new RuntimeException("don't know what to do with token: "+token);
            }
        }
    }
    
    protected ParameterBase wrapper;
    protected Field[] fields;
}