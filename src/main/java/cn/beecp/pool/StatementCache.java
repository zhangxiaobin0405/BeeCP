/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.pool;

import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import static cn.beecp.util.BeecpUtil.oclose;

/**
 * Statement cache
 *
 * @author Chris.liao
 * @version 1.0
 */
class StatementCache extends HashMap<Object,CacheNode> {
	private int capacity;
	private CacheNode head=null;//old
	private CacheNode tail=null;//new
	public StatementCache(int capacity) {
		super(capacity*2);
		this.capacity=capacity;
	}

	public PreparedStatement getPreparedStatement(Object k) {
		CacheNode n = this.get(k);
		if(n == null) return null;

		if(this.size()>1 && n!=tail) {
			//remove from chain
			if (n == head) {//at head
				head = head.next;
			} else {//at middle
				n.pre.next = n.next;
				n.next.pre = n.pre;
			}

			//append to tail
			tail.next = n;
			n.pre = tail;
			n.next = null;
			tail = n;
		}
		return n.v;
	}
	public void putPreparedStatement(Object k,PreparedStatement v) {
		CacheNode n = new CacheNode(k, v);
		this.put(k, n);
		if (head == null) {
			tail = head = n;
		} else {
			tail.next = n;
			n.pre = tail;
			tail = n;

			if (this.size() > capacity) {
				this.remove(head.k);
				oclose(head.v);
				if (head == tail) {
					head = null;
					tail = null;
				} else {
					head = head.next;
				}
			}
		}
	}
	void clearStatement() {
		Iterator<Entry<Object, CacheNode>> iterator=this.entrySet().iterator();
		while (iterator.hasNext()) {
			oclose(iterator.next().getValue().v);
		}
		clear();
		head=null;
		tail=null;
	}
}
class CacheNode {// double linked chain node
	Object k;
	PreparedStatement v;
	CacheNode pre;
	CacheNode next;
	CacheNode(Object k, PreparedStatement v) {
		this.k = k;
		this.v = v;
	}
}
class PsCacheKey{
	private String sql;
	private int autoGeneratedKeys;
	private int[] columnIndexes;
	private String[] columnNames;
	private int resultSetType;
	private int resultSetConcurrency;
	private int resultSetHoldability;

	private int type;
	private int hashCode;
	private final static int prime=31;
	private final static int TYPE1=1;
	private final static int TYPE2=2;
	private final static int TYPE3=3;
	private final static int TYPE4=4;
	private final static int TYPE5=5;
	private final static int TYPE6=6;

	public PsCacheKey(String sql) {
		type=TYPE1;
		this.sql=sql;
		hashCode=sql.hashCode();
	}
	public PsCacheKey(String sql, int autoGeneratedKeys) {
		type=TYPE2;
		this.sql=sql;
		this.autoGeneratedKeys=autoGeneratedKeys;

		hashCode=prime * autoGeneratedKeys+sql.hashCode();
	}
	public PsCacheKey(String sql, int[] columnIndexes) {
		type=TYPE3;
		this.sql=sql;
		this.columnIndexes=columnIndexes;

		hashCode=Arrays.hashCode(columnIndexes);
		hashCode=prime * hashCode+ sql.hashCode();;
	}
	public PsCacheKey(String sql, String[] columnNames) {
		type=TYPE4;
		this.sql=sql;
		this.columnNames=columnNames;

		hashCode=Arrays.hashCode(columnNames);
		hashCode=prime * hashCode+sql.hashCode();
	}
	public PsCacheKey(String sql, int resultSetType, int resultSetConcurrency) {
		type=TYPE5;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;

		hashCode=prime * resultSetType+resultSetConcurrency;
		hashCode=prime * hashCode+sql.hashCode();
	}
	public PsCacheKey(String sql, int resultSetType, int resultSetConcurrency,int resultSetHoldability) {
		type=TYPE6;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;
		this.resultSetHoldability=resultSetHoldability;

		hashCode=prime * resultSetType+resultSetConcurrency;
		hashCode=prime * hashCode+resultSetHoldability;
		hashCode=prime * hashCode+ +sql.hashCode();;
	}

	@Override
	public int hashCode(){
		return hashCode;
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PsCacheKey))return false;
		PsCacheKey other=(PsCacheKey)obj;
		if(this.type!=other.type)return false;
		switch(this.type){
			case TYPE1:return this.sql.equals(other.sql);
			case TYPE2:return autoGeneratedKeys==other.autoGeneratedKeys && this.sql.equals(other.sql);
			case TYPE3:return Arrays.equals(columnIndexes,other.columnIndexes)&& this.sql.equals(other.sql);
			case TYPE4:return Arrays.equals(columnNames,other.columnNames)&& this.sql.equals(other.sql);
			case TYPE5:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && this.sql.equals(other.sql);
			case TYPE6:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && resultSetHoldability==other.resultSetHoldability && this.sql.equals(other.sql);
			default:return false;
		}
	}
}
class CsCacheKey {
	private String sql;
	private int resultSetType;
	private int resultSetConcurrency;
	private int resultSetHoldability;

	private int type;
	private int hashCode;
	private final static int prime=31;
	private final static int TYPE7=7;
	private final static int TYPE8=8;
	private final static int TYPE9=9;

	public CsCacheKey(String sql) {
		type=TYPE7;
		this.sql=sql;
		hashCode=sql.hashCode();
	}
	public CsCacheKey(String sql, int resultSetType, int resultSetConcurrency) {
		type=TYPE8;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;

		hashCode=prime * resultSetType+ resultSetConcurrency;
		hashCode=prime * hashCode+ sql.hashCode();
	}
	public CsCacheKey(String sql, int resultSetType, int resultSetConcurrency,int resultSetHoldability) {
		type=TYPE9;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;
		this.resultSetHoldability=resultSetHoldability;

		hashCode=prime * resultSetType+resultSetConcurrency;
		hashCode=prime * hashCode+resultSetHoldability;
		hashCode=prime * hashCode+ +sql.hashCode();;
	}
	@Override
	public int hashCode(){
		return hashCode;
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof CsCacheKey))return false;
		CsCacheKey other=(CsCacheKey)obj;
		if(this.type!=other.type)return false;

		switch(this.type){
			case TYPE7:return this.sql.equals(other.sql);
			case TYPE8:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && this.sql.equals(other.sql);
			case TYPE9:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && resultSetHoldability==other.resultSetHoldability && this.sql.equals(other.sql);
			default:return false;
		}
	}
}