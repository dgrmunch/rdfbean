package com.mysema.rdfbean.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptionalBlockTest {
    
    @Test
    public void To_String(){
        Block block = Blocks.optional(Blocks.SPOC);
        assertEquals("OPTIONAL { {s} {p} {o} {c} .  }", block.toString());
    }

    @Test
    public void To_String_with_Filter(){
        Block block = Blocks.optionalFilter(Blocks.SPOC, QNODE.o.lit().lt("X"));
        assertEquals("OPTIONAL { {s} {p} {o} {c} .  FILTER({o} < \"X\") }", block.toString());
    }
    
    @Test
    public void Exists(){
        assertEquals("exists OPTIONAL { {s} {p} {o} {c} .  }", Blocks.optional(Blocks.SPOC).exists().toString());
    }
}
