package com.hfturay.battleplugin.ability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on an ability whose numbers live in abilities.yml. It lists every setting the ability reads.
 *
 * The ability has no numbers of its own: if one of these is missing from the file, the ability refuses to
 * run (with a message naming the setting) and /kits reload warns about it, instead of guessing a value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresSettings {
    String[] value();
}
