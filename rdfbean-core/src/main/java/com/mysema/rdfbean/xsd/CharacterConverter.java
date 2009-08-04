package com.mysema.rdfbean.xsd;

/**
 * CharacterConverter provides
 *
 * @author tiwe
 * @version $Id$
 */
public class CharacterConverter extends AbstractConverter<Character> {

    @Override
    public Character fromString(String str) {
        return Character.valueOf(str.charAt(0));
    }

}