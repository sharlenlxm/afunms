package com.afunms.polling.snmp.memory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.afunms.common.util.CheckEventUtil;
import com.afunms.common.util.ShareData;
import com.afunms.common.util.SnmpUtils;
import com.afunms.indicators.model.NodeGatherIndicators;
import com.afunms.monitor.executor.base.SnmpMonitor;
import com.afunms.polling.PollingEngine;
import com.afunms.polling.node.Host;
import com.afunms.polling.om.MemoryCollectEntity;
import com.gatherResulttosql.NetHostMemoryRtsql;
import com.gatherResulttosql.NetmemoryResultTosql;

@SuppressWarnings("unchecked")
public class ZTEMemorySnmp extends SnmpMonitor {
	java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public Hashtable collect_Data(NodeGatherIndicators nodeGatherIndicators) {
		Hashtable returnHash = new Hashtable();
		Vector memoryVector = new Vector();
		List memoryList = new ArrayList();
		Host node = (Host) PollingEngine.getInstance().getNodeByID(Integer.parseInt(nodeGatherIndicators.getNodeid()));
		if (node == null) {
			return null;
		}
		try {
			Calendar date = Calendar.getInstance();
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				com.afunms.polling.base.Node snmpnode = PollingEngine.getInstance().getNodeByIP(node.getIpAddress());
				Date cc = date.getTime();
				String time = sdf.format(cc);
				snmpnode.setLastTime(time);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				String temp = "0";
				int usedvalueperc = 0;
				String memtype = "";// 有两中类型 memp 代表百分比，memsize 代表大小
				if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.")) {
					String[][] valueArray = null;
					String[] oids = new String[] { "1.3.6.1.4.1.2011.6.1.2.1.1.2",// hwMemSize
							"1.3.6.1.4.1.2011.6.1.2.1.1.3"// hwMemFree
					};
					memtype = "memsize";

					if (node.getSysOid().equals("1.3.6.1.4.1.2011.1.1.1.12811") || node.getSysOid().equals("1.3.6.1.4.1.2011.10.1.89")) {

						oids = new String[] { "1.3.6.1.4.1.2011.2.2.5.1",// 已用内存
								"1.3.6.1.4.1.2011.2.2.5.2"// 空闲内存
						};
						memtype = "memsize";
					}

					// ZTE T600
					if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.3.100.27")) {

						oids = new String[] { "1.3.6.1.4.1.3902.3.3.1.1.4"// 内存利用率
						};
						memtype = "memp";

						valueArray = SnmpUtils.getTemperatureTableData(node.getIpAddress(), node.getCommunity(), oids, node.getSnmpversion(), node.getSecuritylevel(), node
								.getSecurityName(), node.getV3_ap(), node.getAuthpassphrase(), node.getV3_privacy(), node.getPrivacyPassphrase(), 3, 1000 * 30);
						int allvalue = 0;

						int flag = 0;
						if (valueArray != null && valueArray.length > 0) {

							// 根据不同的类型来判断
							if (memtype.equals("memp")) {
								for (int i = 0; i < valueArray.length; i++) {
									String _value = valueArray[i][0];
									String index = valueArray[i][1];
									int value = 0;
									try {
										value = (int) Float.parseFloat(_value);
									} catch (Exception e) {
									}
									try {
										allvalue = allvalue + value;
									} catch (Exception e) {
										e.printStackTrace();
									}
									if (value > 0) {
										flag = flag + 1;
										List alist = new ArrayList();
										alist.add(index);
										alist.add(_value);
										memoryList.add(alist);
									}
								}
							}

							if (memtype.equals("memsize")) {
								for (int i = 0; i < valueArray.length; i++) {
									String sizevalue = valueArray[i][0];
									String freevalue = valueArray[i][1];
									String index = valueArray[i][2];
									float value = 0.0f;
									String usedperc = "0";
									if (Long.parseLong(sizevalue) > 0) {
										value = (Long.parseLong(sizevalue) - Long.parseLong(freevalue)) * 100 / (Long.parseLong(sizevalue));
									}

									if (value > 0) {
										int intvalue = Math.round(value);
										allvalue = allvalue + intvalue;
										flag = flag + 1;
										List alist = new ArrayList();
										alist.add("");
										alist.add(usedperc);
										memoryList.add(alist);
										MemoryCollectEntity memorycollectdata = new MemoryCollectEntity();
										memorycollectdata.setIpaddress(node.getIpAddress());
										memorycollectdata.setCollecttime(date);
										memorycollectdata.setCategory("Memory");
										memorycollectdata.setEntity("Utilization");
										memorycollectdata.setSubentity(index);
										memorycollectdata.setRestype("dynamic");
										memorycollectdata.setUnit("%");
										memorycollectdata.setThevalue(intvalue + "");
										memoryVector.addElement(memorycollectdata);
									}
								}
							}

							int result = 0;
							if (flag > 0) {
								usedvalueperc = allvalue / flag;
								temp = usedvalueperc + "";
							}
							if (temp == null) {
								result = 0;
							} else {
								try {
									if (temp.equalsIgnoreCase("noSuchObject")) {
										result = 0;
									} else {
										result = Integer.parseInt(temp);
									}
								} catch (Exception ex) {
									ex.printStackTrace();
									result = 0;
								}
							}

							MemoryCollectEntity memorycollectdata = new MemoryCollectEntity();
							memorycollectdata.setIpaddress(node.getIpAddress());
							memorycollectdata.setCollecttime(date);
							memorycollectdata.setCategory("Memory");
							memorycollectdata.setEntity("Utilization");
							memorycollectdata.setSubentity("Utilization");
							memorycollectdata.setRestype("dynamic");
							memorycollectdata.setUnit("%");
							memorycollectdata.setThevalue(result + "");
							memoryVector.addElement(memorycollectdata);

						}
						// zte2928-2
					} else if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.15.2.30")) {
						oids = new String[] { "1.3.6.1.4.1.3902.15.2.30.1.5"// 内存利用率
						};
						memtype = "memp";

						valueArray = SnmpUtils.getTemperatureTableData(node.getIpAddress(), node.getCommunity(), oids, node.getSnmpversion(), node.getSecuritylevel(), node
								.getSecurityName(), node.getV3_ap(), node.getAuthpassphrase(), node.getV3_privacy(), node.getPrivacyPassphrase(), 3, 1000 * 30);
						int allvalue = 0;

						int flag = 0;
						if (valueArray != null && valueArray.length > 0) {

							// 根据不同的类型来判断
							if (memtype.equals("memp")) {
								for (int i = 0; i < valueArray.length; i++) {
									String _value = valueArray[i][0];
									String index = valueArray[i][1];
									int value = 0;
									try {
										value = (int) Float.parseFloat(_value);
									} catch (Exception e) {
									}
									try {
										allvalue = allvalue + value;
									} catch (Exception e) {
										e.printStackTrace();
									}
									if (value > 0) {
										flag = flag + 1;
										List alist = new ArrayList();
										alist.add(index);
										alist.add(_value);
										memoryList.add(alist);
									}
								}
							}

							if (memtype.equals("memsize")) {
								for (int i = 0; i < valueArray.length; i++) {
									String sizevalue = valueArray[i][0];
									String freevalue = valueArray[i][1];
									String index = valueArray[i][2];
									float value = 0.0f;
									String usedperc = "0";
									if (Long.parseLong(sizevalue) > 0) {
										value = (Long.parseLong(sizevalue) - Long.parseLong(freevalue)) * 100 / (Long.parseLong(sizevalue));
									}

									if (value > 0) {
										int intvalue = Math.round(value);
										allvalue = allvalue + intvalue;
										flag = flag + 1;
										List alist = new ArrayList();
										alist.add("");
										alist.add(usedperc);
										memoryList.add(alist);
										MemoryCollectEntity memorycollectdata = new MemoryCollectEntity();
										memorycollectdata.setIpaddress(node.getIpAddress());
										memorycollectdata.setCollecttime(date);
										memorycollectdata.setCategory("Memory");
										memorycollectdata.setEntity("Utilization");
										memorycollectdata.setSubentity(index);
										memorycollectdata.setRestype("dynamic");
										memorycollectdata.setUnit("%");
										memorycollectdata.setThevalue(intvalue + "");
										memoryVector.addElement(memorycollectdata);
									}
								}
							}

							int result = 0;
							if (flag > 0) {
								usedvalueperc = allvalue / flag;
								temp = usedvalueperc + "";
							}
							if (temp == null) {
								result = 0;
							} else {
								try {
									if (temp.equalsIgnoreCase("noSuchObject")) {
										result = 0;
									} else {
										result = Integer.parseInt(temp);
									}
								} catch (Exception ex) {
									ex.printStackTrace();
									result = 0;
								}
							}

							MemoryCollectEntity memorycollectdata = new MemoryCollectEntity();
							memorycollectdata.setIpaddress(node.getIpAddress());
							memorycollectdata.setCollecttime(date);
							memorycollectdata.setCategory("Memory");
							memorycollectdata.setEntity("Utilization");
							memorycollectdata.setSubentity("Utilization");
							memorycollectdata.setRestype("dynamic");
							memorycollectdata.setUnit("%");
							memorycollectdata.setThevalue(result + "");
							memoryVector.addElement(memorycollectdata);

						}

					} else {
						// ZTE M6000
						if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.3.100.6002.2")) {

							oids = new String[] { "1.3.6.1.4.1.3902.3.6002.2.1.1.6"// 内存利用率
							};
							memtype = "memp";
						}

						// ZTE 5928
						if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.3.100.40")) {

							oids = new String[] { "1.3.6.1.4.1.3902.3.3.1.1.4"// 内存利用率
							};
							memtype = "memp";
						}

						// ZTE 3884
						if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.3.100.135")) {

							oids = new String[] { "1.3.6.1.4.1.3902.3.3.1.1.4"// 内存利用率
							};
							memtype = "memp";
						}

						// ZTE 2928
						if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.15.2.11.2")) {

							oids = new String[] { "1.3.6.1.4.1.3902.15.2.11.1.5"// 内存利用率
							};
							memtype = "memp";
						}
						// zte 2609
						if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.15.2.1.4")) {

							oids = new String[] { "1.3.6.1.4.1.3902.15.2.2.1.5"// 内存利用率
							};
							memtype = "memp";
						}

						valueArray = SnmpUtils.getTemperatureTableData(node.getIpAddress(), node.getCommunity(), oids, node.getSnmpversion(), node.getSecuritylevel(), node
								.getSecurityName(), node.getV3_ap(), node.getAuthpassphrase(), node.getV3_privacy(), node.getPrivacyPassphrase(), 3, 1000 * 30);
						int allvalue = 0;

						int flag = 0;
						if (valueArray != null && valueArray.length > 0) {

							// 根据不同的类型来判断
							if (memtype.equals("memp")) {
								for (int i = 0; i < valueArray.length; i++) {
									String _value = valueArray[i][0];
									String index = valueArray[i][1];
									int value = 0;
									try {
										value = Integer.parseInt(_value);
									} catch (Exception e) {
									}
									if (node.getSysOid().startsWith("1.3.6.1.4.1.3902.15.2.1.4")) {
										value = 100 - value;
									}
									allvalue = allvalue + value;
									if (value > 0) {
										flag = flag + 1;
										List alist = new ArrayList();
										alist.add(index);
										alist.add(_value);
										memoryList.add(alist);

									}
								}

							}

							if (memtype.equals("memsize")) {
								for (int i = 0; i < valueArray.length; i++) {
									String sizevalue = valueArray[i][0];
									String freevalue = valueArray[i][1];
									String index = valueArray[i][2];
									float value = 0.0f;
									String usedperc = "0";
									if (Long.parseLong(sizevalue) > 0) {
										value = (Long.parseLong(sizevalue) - Long.parseLong(freevalue)) * 100 / (Long.parseLong(sizevalue));
									}

									if (value > 0) {
										int intvalue = Math.round(value);
										allvalue = allvalue + intvalue;
										flag = flag + 1;
										List alist = new ArrayList();
										alist.add("");
										alist.add(usedperc);
										memoryList.add(alist);
										MemoryCollectEntity memorycollectdata = new MemoryCollectEntity();
										memorycollectdata.setIpaddress(node.getIpAddress());
										memorycollectdata.setCollecttime(date);
										memorycollectdata.setCategory("Memory");
										memorycollectdata.setEntity("Utilization");
										memorycollectdata.setSubentity(index);
										memorycollectdata.setRestype("dynamic");
										memorycollectdata.setUnit("%");
										memorycollectdata.setThevalue(intvalue + "");
										memoryVector.addElement(memorycollectdata);
									}
								}
							}

							int result = 0;
							if (flag > 0) {
								usedvalueperc = allvalue / flag;
								temp = usedvalueperc + "";
							}
							if (temp == null) {
								result = 0;
							} else {
								try {
									if (temp.equalsIgnoreCase("noSuchObject")) {
										result = 0;
									} else {
										result = Integer.parseInt(temp);
									}
								} catch (Exception ex) {
									ex.printStackTrace();
									result = 0;
								}
							}

							MemoryCollectEntity memorycollectdata = new MemoryCollectEntity();
							memorycollectdata.setIpaddress(node.getIpAddress());
							memorycollectdata.setCollecttime(date);
							memorycollectdata.setCategory("Memory");
							memorycollectdata.setEntity("Utilization");
							memorycollectdata.setSubentity("Utilization");
							memorycollectdata.setRestype("dynamic");
							memorycollectdata.setUnit("%");
							memorycollectdata.setThevalue(result + "");
							memoryVector.addElement(memorycollectdata);

						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Hashtable ipAllData = new Hashtable();
		try {
			ipAllData = (Hashtable) ShareData.getSharedata().get(node.getIpAddress());
		} catch (Exception e) {

		}
		if (ipAllData == null) {
			ipAllData = new Hashtable();
		}
		if (memoryVector != null && memoryVector.size() > 0) {
			ipAllData.put("memory", memoryVector);
		}
		ShareData.getSharedata().put(node.getIpAddress(), ipAllData);
		returnHash.put("memory", memoryVector);

		Hashtable collectHash = new Hashtable();
		collectHash.put("memory", memoryVector);

		try {
			if (memoryVector != null && memoryVector.size() > 0) {
				int thevalue = 0;
				for (int i = 0; i < memoryVector.size(); i++) {
					MemoryCollectEntity memorycollectdata = (MemoryCollectEntity) memoryVector.get(i);
					if ("Utilization".equals(memorycollectdata.getEntity())) {
						if (Integer.parseInt(memorycollectdata.getThevalue()) > thevalue) {
							thevalue = Integer.parseInt(memorycollectdata.getThevalue());
						}
					}
				}
				CheckEventUtil checkutil = new CheckEventUtil();
				checkutil.updateData(node, nodeGatherIndicators, thevalue + "", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		NetmemoryResultTosql tosql = new NetmemoryResultTosql();
		tosql.CreateResultTosql(returnHash, node.getIpAddress());
		NetHostMemoryRtsql totempsql = new NetHostMemoryRtsql();
		totempsql.CreateResultTosql(returnHash, node);

		return returnHash;
	}
}
