package com.dianping.cat.report.page.overload.task;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.dal.jdbc.DalException;
import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Cat;
import com.dianping.cat.core.config.Config;
import com.dianping.cat.core.config.ConfigDao;
import com.dianping.cat.core.config.ConfigEntity;
import com.dianping.cat.home.dal.report.OverloadDao;
import com.dianping.cat.home.dal.report.OverloadEntity;

public class CapacityUpdateStatusManager implements Initializable {

	@Inject
	private ConfigDao m_configDao;

	@Inject
	private OverloadDao m_overloadDao;

	private static final String CONFIG_NAME = "capacityUpdateStatus";

	private int m_hourlyStatus;

	private int m_dailyStatus;

	private int m_weeklyStatus;

	private int m_monthlyStatus;

	private int m_configId;

	private String buildConfigContent() {
		StringBuilder builder = new StringBuilder();

		builder.append("Hourly:").append(m_hourlyStatus).append(";");
		builder.append("Daily:").append(m_dailyStatus).append(";");
		builder.append("Weekly:").append(m_weeklyStatus).append(";");
		builder.append("Monthly:").append(m_monthlyStatus).append(";");
		return builder.toString();
	}

	private void extractStatus(String content) {
		m_hourlyStatus = Integer.parseInt(content.split("Hourly:")[1].split(";")[0]);
		m_dailyStatus = Integer.parseInt(content.split("Daily:")[1].split(";")[0]);
		m_weeklyStatus = Integer.parseInt(content.split("Weekly:")[1].split(";")[0]);
		m_monthlyStatus = Integer.parseInt(content.split("Monthly:")[1].split(";")[0]);
	}

	public int getDailyStatus() {
		return m_dailyStatus;
	}

	public int getHourlyStatus() {
		return m_hourlyStatus;
	}

	public int getMonthlyStatus() {
		return m_monthlyStatus;
	}

	public int getWeeklyStatus() {
		return m_weeklyStatus;
	}

	@Override
	public void initialize() throws InitializationException {
		try {
			Config config = m_configDao.findByName(CONFIG_NAME, ConfigEntity.READSET_FULL);
			String content = config.getContent();
			m_configId = config.getId();

			extractStatus(content);
		} catch (DalException e) {
			try {
				m_hourlyStatus = m_overloadDao.findMaxIdByType(CapacityUpdater.HOURLY_TYPE, OverloadEntity.READSET_MAXID)
				      .getMaxId();
				m_dailyStatus = m_overloadDao.findMaxIdByType(CapacityUpdater.DAILY_TYPE, OverloadEntity.READSET_MAXID)
				      .getMaxId();
				m_weeklyStatus = m_overloadDao.findMaxIdByType(CapacityUpdater.WEEKLY_TYPE, OverloadEntity.READSET_MAXID)
				      .getMaxId();
				m_monthlyStatus = m_overloadDao.findMaxIdByType(CapacityUpdater.MONTHLY_TYPE, OverloadEntity.READSET_MAXID)
				      .getMaxId();

				Config config = m_configDao.createLocal();

				config.setName(CONFIG_NAME);
				config.setContent(buildConfigContent());
				m_configDao.insert(config);

				m_configId = config.getId();
			} catch (DalException ex) {
				Cat.logError(ex);
			}
		}
	}

	private boolean storeConfig() {
		synchronized (this) {
			try {
				Config config = m_configDao.createLocal();

				config.setId(m_configId);
				config.setKeyId(m_configId);
				config.setName(CONFIG_NAME);
				config.setContent(buildConfigContent());
				m_configDao.updateByPK(config, ConfigEntity.UPDATESET_FULL);
			} catch (Exception e) {
				Cat.logError(e);
				return false;
			}
		}
		return true;
	}

	public void updateDailyStatus(int dailyStatus) {
		this.m_dailyStatus = dailyStatus;
		storeConfig();
	}

	public void updateHourlyStatus(int hourlyStatus) {
		this.m_hourlyStatus = hourlyStatus;
		storeConfig();
	}

	public void updateMonthlyStatus(int monthlyStatus) {
		this.m_monthlyStatus = monthlyStatus;
		storeConfig();
	}

	public void updateWeeklyStatus(int weeklyStatus) {
		this.m_weeklyStatus = weeklyStatus;
		storeConfig();
	}

}
