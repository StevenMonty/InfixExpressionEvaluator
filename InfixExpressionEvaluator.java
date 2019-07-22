/**
 * @author Steven Montalbano
 * 
 * Shunting Yard Algorithm - Infix to Post Fix Expression Evaluator
 */
package a2;	//TODO FIX PACKAGE AND RUN FORM TERMINAL BEFORE TURNING IN

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

/*
 * ~ ~ ~TODO~ ~ ~
 * Key Features:
 * 		Properly matched brackets - DONE
 * 		Fix order of operations	 - DONE
 * 
 * Error Checking:
 *		Divide by zero error - DONE
 * 		Back to back operators - DONE
 * 		Mismatched Brackets	- DONE
 * 		Open bracket followed by operatoR - DONE 
 * 		Close bracket with no open bracket - DONE
 * 
 * Extra Credit:
 * 		Coefficient multiplication ex: 2(4/2) = 4 - DONE
 * 		Hexacedimal operations - "0x6a" = 106 - DONE
 */

/**
 * This class uses two stacks to evaluate an infix arithmetic expression from an
 * InputStream. It should not create a full postfix expression along the way; it
 * should convert and evaluate in a pipelined fashion, in a single pass.
 */
public class InfixExpressionEvaluator {
	
	static boolean debuggingMode = false;		//TODO set to false before turning in
	
	// Boolean to track if the last and current token being processed are 
	// operators for back to back operator error checking in evaluate method
	// true == operator, false == operand
	static char lastTokenValue;
	static boolean lastToken, thisToken;
	static int tokenCount = 0, openBrackets = 0, closeBrackets = 0;
	
    // Tokenizer to break up our input into tokens
    StreamTokenizer tokenizer;

    // Stacks for operators (for converting to postfix) and operands (for
    // evaluating)
    StackInterface<Character> operatorStack;
    StackInterface<Double> operandStack;

    /**
     * Initializes the evaluator to read an infix expression from an input
     * stream.
     * @param input the input stream from which to read the expression
     */
    public InfixExpressionEvaluator(InputStream input) {
        // Initialize the tokenizer to read from the given InputStream
        tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(input)));

        // StreamTokenizer likes to consider - and / to have special meaning.
        // Tell it that these are regular characters, so that they can be parsed
        // as operators
        tokenizer.ordinaryChar('-');
        tokenizer.ordinaryChar('/');

        // Allow the tokenizer to recognize end-of-line, which marks the end of
        // the expression
        tokenizer.eolIsSignificant(true);

        // Initialize the stacks
        operatorStack = new ArrayStack<Character>();
        operandStack = new ArrayStack<Double>();
    }

    /**
     * Parses and evaluates the expression read from the provided input stream,
     * then returns the resulting value
     * @return the value of the infix expression that was parsed
     */
    public Double evaluate() throws InvalidExpressionException {
        // Get the first token. If an IO exception occurs, replace it with a
        // runtime exception, causing an immediate crash.
        try {
            tokenizer.nextToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Continue processing tokens until we find end-of-line
        while (tokenizer.ttype != StreamTokenizer.TT_EOL) {

        	//Check for back to back operators
        	if ("+-*/^".contains(String.valueOf((char)tokenizer.ttype)))
        		thisToken = true;
        	else 
        		thisToken = false;
	  	
        	if (thisToken && lastToken)
	        		throw new InvalidExpressionException("BACK TO BACK OPERATORS:" 
	        					+ operatorStack.peek() + String.valueOf((char)tokenizer.ttype));
        	
        	lastToken = thisToken;
        	tokenCount++;
        	
        	// Consider possible token types
            switch (tokenizer.ttype) {
                case StreamTokenizer.TT_NUMBER:
                    // If the token is a number, process it as a double-valued
                    // operand
                    handleOperand((double)tokenizer.nval);
                    break;
                case '+':
                case '-':
                case '*':
                case '/':
                case '^':      
                    // If the token is any of the above characters, process it
                    // is an operator
                    handleOperator((char)tokenizer.ttype);
                    break;
                case '(':
                case '{':
                    // If the token is open bracket, process it as such. Forms
                    // of bracket are interchangeable but must nest properly.
                	openBrackets++;
                    handleOpenBracket((char)tokenizer.ttype);
                    break;
                case ')':
                case '}':
                    // If the token is close bracket, process it as such. Forms
                    // of bracket are interchangeable but must nest properly.
                	closeBrackets++;
                    handleCloseBracket((char)tokenizer.ttype);
                    break;
                case StreamTokenizer.TT_WORD:
                    // If the token is a "word", throw an expression error, if in hex format then decode and handle
                	try {
                			handleOperand(Integer.decode(operandStack.pop().intValue() + tokenizer.sval)); 	//HEX CONVERSION
                		} 
                	catch (Exception e) {	
                		throw new InvalidExpressionException("Unrecognized symbol: " + tokenizer.sval);
                	}   
                	break;
                default:
                    // If the token is any other type or value, throw an
                    // expression error
                    throw new InvalidExpressionException("Unrecognized symbol: " +
                                    String.valueOf((char)tokenizer.ttype));
            }
            
            debugPrint(String.valueOf((char)tokenizer.ttype));
            debugPrint(operandStack.toString());
            debugPrint(operatorStack.toString());
 
            // Read the next token, again converting any potential IO exception
            try {
                tokenizer.nextToken();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Almost done now, but we may have to process remaining operators in
        // the operators stack
        handleRemainingOperators();
        
        // Return the result of the evaluation
        return operandStack.peek();	
    }

	/**
     * This method is called when the evaluator encounters an operand. It
     * manipulates operatorStack and/or operandStack to process the operand
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param operand the operand token that was encountered
     */
    void handleOperand(double operand) {
    	
    	//Trailing coefficient multiplication ex: (4/2)2 = 4
    	if (lastTokenValue == ')' || lastTokenValue == '}')	
    		operatorStack.push('*');
    	
    	operandStack.push(operand);
    	
    	//Reset last token to empty char in the case of encountering a number to 
    	//avoid a false error being thrown
    	lastTokenValue = ' ';
    }

    /**
     * This method is called when the evaluator encounters an operator. It
     * manipulates operatorStack and/or operandStack to process the operator
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param operator the operator token that was encountered
     */
    void handleOperator(char operator) {
    	
    	//Case where and operator is the first token in an expression
    	if (tokenCount == 1)
    		throw new InvalidExpressionException("CANNOT START EXPRESSION WITH OPERATOR");
    		
    	//Bracket followed by operator 
    	if (lastTokenValue == '(' || lastTokenValue == '{')
    		throw new InvalidExpressionException("BRACKET FOLLOWED BY OPERATOR");
    	
    	int currentPrecedence = getPrecedence(operator);
    	
        debugPrint(String.valueOf(operator));
    	
        //Perform calculations while the stack is not empty and the precedence of the operator being passed in
        //is <= to the precidence of the operator on top of the stack
    	while (!operatorStack.isEmpty() && currentPrecedence <= getPrecedence(operatorStack.peek()))	
    			calculate();
    	   	
    	operatorStack.push(operator);
    	
    	lastTokenValue = operator;
    }

    /**
     * This method is called when the evaluator encounters an open bracket. It
     * manipulates operatorStack and/or operandStack to process the open bracket
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param openBracket the open bracket token that was encountered
     */
    void handleOpenBracket(char openBracket) {

    	// If last token is false, then an operator was encountered before the open bracket was
    	// encountered, inferring that the user was attempting to do coefficient multiplication,
    	// adds * sign to stack before the bracket to account for this. 
    	if (!lastToken  && tokenCount > 1 && lastTokenValue == ' ')
    		operatorStack.push('*');
    			
    	operatorStack.push(openBracket);	
    	
    	lastTokenValue = openBracket;
    }

    /**
     * This method is called when the evaluator encounters a close bracket. It
     * manipulates operatorStack and/or operandStack to process the close
     * bracket according to the Infix-to-Postfix and Postfix-evaluation
     * algorithms.
     * @param closeBracket the close bracket token that was encountered
     */
    void handleCloseBracket(char closeBracket) {    	
 	
    	// Check for empty brackets not containing an operand
    	if (operatorStack.peek() == '(' && lastTokenValue != ' ' || operatorStack.peek() == '{' && lastTokenValue != ' ' )
    		throw new InvalidExpressionException("EMPTY BRACKETS");
   	
    	if (closeBracket == ')')
    		while (operatorStack.peek() != '(' ) 
    		{
    			calculate();
    			
    			if (operatorStack.peek() == '{' || operatorStack.peek() == '}')	//encounter wrong type of bracket
    				throw new InvalidExpressionException("MISMATCHED BRACKETS");
    		}

    	else if (closeBracket == '}')
    		while (operatorStack.peek() != '{' ) 
    		{
    			calculate();
    			
    			if (operatorStack.peek() == '(' || operatorStack.peek() == ')')	//encounter wrong type of bracket
    				throw new InvalidExpressionException("MISMATCHED BRACKETS");
    		}
	
    	operatorStack.pop();	//Calculate until the open bracket is on top of the stack, then remove it
    	lastTokenValue = closeBracket;
    }

    /**
     * This method is called when the evaluator encounters the end of an
     * expression. It manipulates operatorStack and/or operandStack to process
     * the operators that remain on the stack, according to the Infix-to-Postfix
     * and Postfix-evaluation algorithms.
     */
    void handleRemainingOperators() {
	
    	while (!operatorStack.isEmpty())
    		calculate();
    }
    /**
     * Method to perform the mathematical operations by popping off the Operator and Operand stack
     */
    public void calculate()
    {       	
    	//Check to make sure there are the same number of open and close brackets once the 
    	//tokenizer has reached the end of the expression
    	if (tokenizer.ttype == StreamTokenizer.TT_EOL && openBrackets != closeBrackets)
        	throw new InvalidExpressionException("MISSING BRACKET(S)");
    	
    	//Check to make sure the last token is not an operator or closing bracket and 
    	//the tokenizer has reached the end of the expression
    	if(lastTokenValue != ')' && lastTokenValue != '}')
    		if (tokenizer.ttype == StreamTokenizer.TT_EOL && lastTokenValue != ' ')
    			throw new InvalidExpressionException("CANNOT END EXPRESSION WITH OPERATOR");
        
    	double current = operandStack.pop();
    	double prev = operandStack.pop();
        
        debugPrint("prev " + prev);
        debugPrint("current " + current);

		switch(operatorStack.pop())
		{
    		case '^':	
    			operandStack.push(Math.pow(prev, current)); break;
    		case '*': 
    			operandStack.push(prev * current);			break;	
    		case '/':	
    			if (current != 0)
	    			{
	    				operandStack.push(prev / current);
	    				break;
	    			}
    			else throw new InvalidExpressionException("DIVIDE BY ZERO");
    		case '+':	
    			operandStack.push(prev + current);			break;
    		case '-':   
    			operandStack.push(prev - current);			break;		
		}
    }
   
    /**
     * @param operator the mathematical operation being evaluated by calculate()
     * @return the precedence of the operator passed in
     */
    public static int getPrecedence (char operator)	
    {
    	switch(operator) 
    	{
    		case '(': 	return 1;
    		case ')':	return 1;
    		case '{':	return 1;
    		case '}': 	return 1;
    		case '+':	return 2;	
    		case '-':   return 2; 	
    		case '*': 	return 3;
    		case '/':	return 3;
    		case '^':	return 4;
    		default :   throw new IllegalArgumentException("Cannot get precedence of operator " + operator);
    	}	
    }
    
    /**
     * Method that accepts a String for the purpose of priting stacks and other
     * variables while global variable debuggingMode == true
     * @param e the string to be printed
     */
    
    static void debugPrint(String e)
    {
    	if (debuggingMode) System.out.println(e);
    }    

    /**
     * Creates an InfixExpressionEvaluator object to read from System.in, then
     * evaluates its input and prints the result.
     * @param args not used
     */
    public static void main(String[] args) {
        System.out.println("Infix expression:");
        InfixExpressionEvaluator evaluator =
                        new InfixExpressionEvaluator(System.in);
        Double value = null;
        try {
            value = evaluator.evaluate();
        } catch (InvalidExpressionException e) {
            System.out.println("Invalid expression: " + e.getMessage());
        }
        if (value != null) {
            System.out.println(value);
        }
    }
}