package net.sourceforge.squirrel_sql.client.session;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;

interface WeakSessionProxy
{
   ISession session();

   static ISession newProxy(ISession session)
   {
      // Construct dynamic proxy so we don't have to update this class when
      // ISession interface changes.
      WeakReference<ISession> ref = new WeakReference<>(Objects.requireNonNull(session));
      InvocationHandler handler = (proxy, method, args) ->
      {
         if (args == null && method.getName().equals("hashCode")) {
            return System.identityHashCode(proxy);
         } else if (args != null && args.length == 1
               && method.getName().equals("equals")
               && method.getReturnType() == Boolean.TYPE) {
            return proxy == args[0];
         }

         ISession delegate = ref.get();
         if (delegate == null)
         {
            if (args == null && method.getName().equals("toString"))
            {
               return proxy.toString();
            }
            throw new IllegalStateException("closed session");
         }
         else if (args == null && method.getName().equals("session"))
         {
            return delegate;
         }
         return method.invoke(delegate, args);
      };

      return (ISession) Proxy
            .newProxyInstance(ISession.class.getClassLoader(),
                              new Class[] { ISession.class, WeakSessionProxy.class },
                              handler);
   }
}
