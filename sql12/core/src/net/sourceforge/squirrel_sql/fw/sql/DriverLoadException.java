package net.sourceforge.squirrel_sql.fw.sql;

/**
 * Thrown to indicate problem loading JDBC driver.  The {@code cause} will
 * provide specific reason.
 */
public class DriverLoadException extends RuntimeException
{
   private static final long serialVersionUID = -4773127738139804161L;

   public DriverLoadException(Throwable cause)
   {
      super(cause);
   }

   public DriverLoadException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
