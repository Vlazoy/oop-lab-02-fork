import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

// Аннотация с целочисленным параметром — количеством вызовов.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Repeat {
  int value();
}

class Person {
  private final String name;

  public Person() {
    this.name = "Артур";
  }

  @Override
  public String toString() {
    return name;
  }
}

class DemoMethods {

  @Repeat(3)
  public void publicMethod1(String text) {
    System.out.println("publicMethod1: " + text);
  }

  public void publicMethod2(int number) {
    System.out.println("publicMethod2: " + number);
  }

  @Repeat(2)
  protected void protectedMethod1(String text, int number) {
    System.out.println(
            "protectedMethod1: text=" + text + ", number=" + number
    );
  }

  @Repeat(3)
  protected void protectedMethod2(double value, boolean enabled) {
    System.out.println(
            "protectedMethod2: value=" + value + ", enabled=" + enabled
    );
  }

  @Repeat(2)
  private void privateMethod1(Person person) {
    System.out.println("privateMethod1: person=" + person);
  }

  @Repeat(1)
  private void privateMethod2(
          String text,
          int number,
          Person person
  ) {
    System.out.println(
            "privateMethod2: text=" + text
                    + ", number=" + number
                    + ", person=" + person
    );
  }
}

class ArgumentFactory {

  public static Object createArgument(Class<?> type)
          throws ReflectiveOperationException {

    if (type == String.class) {
      return "Пример";
    }

    if (type == int.class || type == Integer.class) {
      return 10;
    }

    if (type == long.class || type == Long.class) {
      return 10L;
    }

    if (type == double.class || type == Double.class) {
      return 2.5;
    }

    if (type == float.class || type == Float.class) {
      return 2.5f;
    }

    if (type == boolean.class || type == Boolean.class) {
      return true;
    }

    if (type == char.class || type == Character.class) {
      return 'A';
    }

    if (type == byte.class || type == Byte.class) {
      return (byte) 1;
    }

    if (type == short.class || type == Short.class) {
      return (short) 2;
    }

    // Для массива создаём пустой массив нужного типа.
    if (type.isArray()) {
      return Array.newInstance(type.getComponentType(), 0);
    }

    // Для перечисления выбираем первую константу.
    if (type.isEnum()) {
      Object[] constants = type.getEnumConstants();

      if (constants.length == 0) {
        throw new IllegalArgumentException(
                "Перечисление не содержит констант: "
                        + type.getName()
        );
      }

      return constants[0];
    }

    // Нельзя напрямую создать интерфейс или абстрактный класс.
    if (type.isInterface()
            || Modifier.isAbstract(type.getModifiers())) {
      throw new IllegalArgumentException(
              "Нужен отдельный способ создания объекта типа: "
                      + type.getName()
      );
    }

    // Остальные объекты создаём через конструктор без параметров.
    Constructor<?> constructor = type.getDeclaredConstructor();
    constructor.setAccessible(true);

    return constructor.newInstance();
  }
}

// Отдельный класс, который находит и вызывает методы.
class AnnotationRunner {

  public static void run(Object object)
          throws ReflectiveOperationException {

    Method[] methods = object.getClass().getDeclaredMethods();

    Arrays.sort(
            methods,
            Comparator.comparing(Method::getName)
    );

    for (Method method : methods) {
      int modifiers = method.getModifiers();

      if (!Modifier.isProtected(modifiers)
              && !Modifier.isPrivate(modifiers)) {
        continue;
      }

      if (!method.isAnnotationPresent(Repeat.class)) {
        continue;
      }

      Repeat annotation = method.getAnnotation(Repeat.class);
      int count = annotation.value();

      if (count < 0) {
        throw new IllegalArgumentException(
                "Отрицательное количество вызовов у метода: "
                        + method.getName()
        );
      }

      System.out.println(
              "\nМетод: " + method.getName()
                      + ", количество вызовов: " + count
      );

      if (count == 0) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      Object[] arguments = new Object[parameterTypes.length];

      for (int i = 0; i < parameterTypes.length; i++) {
        arguments[i] =
                ArgumentFactory.createArgument(parameterTypes[i]);
      }

      method.setAccessible(true);

      for (int i = 0; i < count; i++) {
        method.invoke(object, arguments);
      }
    }
  }
}

public class Main {
  public static void main(String[] args) {
    DemoMethods demo = new DemoMethods();

    try {
      AnnotationRunner.run(demo);
    } catch (InvocationTargetException exception) {
      System.err.println(
              "Ошибка внутри вызванного метода: "
                      + exception.getCause()
      );
      exception.getCause().printStackTrace();
    } catch (ReflectiveOperationException
             | IllegalArgumentException exception) {
      exception.printStackTrace();
    }
  }
}