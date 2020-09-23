class Epicurve2 {
    constructor(svg, width, height, xScale, yScale, xAxis, yAxis, yScale2, yAxis2) {
        this.svg = svg;
        this.width = width;
        this.height = height;
        this.xScale = xScale;
        this.yScale = yScale;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.yScale2 = yScale2;
        this.yAxis2 = yAxis2;
    }

    updateBarChart(data, xCallback, yCallback, clazz, color, highlightColor) {
        let selectedData = this.svg.selectAll("." + clazz).data(data);
        let enterData = selectedData.enter();

        enterData.append("rect")
            .style("shape-rendering", "crispEdges")
            .attr("class", clazz)
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
            .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
            .attr("width", this.xScale.bandwidth())
            .style("fill", d => yCallback(d) > 0 ? color : highlightColor);

        selectedData.merge(selectedData).transition().duration(100)
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
            .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
            .style("fill", d => yCallback(d) > 0 ? color : highlightColor);

        selectedData.exit().remove();

        // Ensure notable dates lines are on top!
        this.svg.selectAll("." + clazz).moveToFront();
        this.svg.selectAll("line").moveToFront();
        this.svg.selectAll('.mouseoverclazz').moveToFront();
    }
}

function getCountyFromUrl(url) {
    let selectedCounty = new URLSearchParams(url).get("county");

    if (!selectedCounty) {
        selectedCounty = "georgia";
    }

    return selectedCounty;
}

function constructCountyUrl(type, county) {
    return "reports/v2/csv/" + county + "/" + type + "_" + county + ".csv"
}

function parseLagData(summaryData, caseDeltaData, caseHistData, caseHistCumData) {
    let tempChartData = [];
    let filteredSummaryData = summaryData.filter(d => d.reportDate >= "2020-05-11");

    for (let i = 0; i < caseHistData.length; i++) {
        let currentCaseHistData = caseHistData[i];
        let currentCaseHistCumData = caseHistCumData[i];
        let currentReportDateString = currentCaseHistData["reportDate"];
        let currentReportDateObj = new Date(currentReportDateString);
        currentReportDateObj.setDate(new Date(currentReportDateString).getDate() + 1);
        let currentSummary = filteredSummaryData.filter(d => d.reportDate === currentReportDateString)[0];
        let currentId = currentSummary.id;
        let currentDeltaData = caseDeltaData.filter(d => d.id === currentId)[0];

        let currentTimeseries = tempChartData[currentId];
        if (!currentTimeseries) {
            currentTimeseries = [];
            tempChartData[currentId] = currentTimeseries;
        }

        for (let j = 0; j < 100; j++) {
            let currentLabelDate = getFormattedDate(currentReportDateObj);
            currentTimeseries.push({
                label: currentLabelDate,
                casesHist: +currentCaseHistData[j],
                casesHistCum: +currentCaseHistCumData[j],
                casesDelta: +currentDeltaData[currentLabelDate],
            });
            currentReportDateObj.setDate(currentReportDateObj.getDate() - 1);
        }
    }

    return tempChartData;
}