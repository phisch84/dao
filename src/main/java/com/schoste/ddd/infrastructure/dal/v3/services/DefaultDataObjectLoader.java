package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

public interface DefaultDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends DataObjectLoader<SR, ID, DO>
{

}
