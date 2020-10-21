class Epicurve {

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
        this.prevNode = {};
        this.currentNode = {};
        this.monthNames = ["January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        ];

        let selectedData = this.svg.selectAll(".prelim-region").data(["2020-08-08"]);
        let enterData = selectedData.enter();

        // draw initial region
        enterData
            .append("rect")
            .style("shape-rendering", "crispEdges")
            .attr("class", "prelim-region")
            .attr("x", d => this.xScale(d))
            .attr("y", d => 0)
            .attr("height", d => this.height)
            .attr("width", this.xScale.bandwidth())
            .style("fill", "#c6d1ff")
            .attr("opacity", "0");
    }

    initDateSelect(xScale) {
        let constructorThis = this;
        let lastAvailableDate = xScale.domain()[xScale.domain().length - 1]

        let selectedMouseDate = this.svg.selectAll(".mouse-date").data([lastAvailableDate]);
        selectedMouseDate.enter().append("svg:rect")
            .attr("x", -this.height - 70)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 80)
            .attr("class", "mouse-date")
            .attr("width", 70)
            .attr("height", 10)
            .attr("transform", "translate(-5,1)rotate(-90)")
            .style("fill", "#103052")
            .attr("opacity", "0");

        selectedMouseDate.enter().append("text")
            .attr("x", -this.height - 53)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 96)
            .attr("transform", "translate(9,0)rotate(-90)")
            .attr("text-anchor", "start")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("class", "mouse-date")
            .style("letter-spacing", "0px")
            .style("color", "white")
            .style("font", "10px sans-serif")
            .attr("opacity", "0")
            .text(d => d);

        let clickedMouseDate = this.svg.selectAll(".clicked-mouse-date").data([lastAvailableDate]);
        clickedMouseDate.enter().append("svg:rect")
            .attr("x", -this.height - 70)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 80)
            .attr("class", "clicked-mouse-date")
            .attr("width", 70)
            .attr("height", 12)
            .attr("transform", "translate(-5,1)rotate(-90)")
            .style("fill", "#103052")
            .attr("opacity", "0");

        clickedMouseDate.enter().append("text")
            .attr("x", -this.height - 53)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 96)
            .attr("transform", "translate(9,0)rotate(-90)")
            .attr("text-anchor", "start")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("class", "clicked-mouse-date")
            .style("letter-spacing", "0px")
            .style("color", "white")
            .style("font", "10px sans-serif")
            .attr("opacity", "0")
            .text(d => d);

        let clickedMouseDateLine = this.svg.selectAll(".clicked-mouse-date-line").data([lastAvailableDate]);
        clickedMouseDateLine.enter().append("line")
            .attr("class", "clicked-mouse-date-line")
            .style("stroke", "white")
            .style("stroke-width", 1)
            .style("shape-rendering", "crispEdges")
            .attr("opacity", "0")
            .attr("x1", d => xScale(0))
            .attr("x2", d => xScale(0))
            .attr("y1", d => this.yScale(0) + 9)
            .attr("y2", d => this.yScale(0) + 14);

        let drag = d3.drag()
            .on('drag', function (d) {
                let mouse = d3.mouse(this);
                let scaledX = getDateAtMouse(mouse, xScale);
                handleMouseClick(scaledX, xScale);

                let data = d3.selectAll(".caseline").filter(d => d.label === scaledX).data()[0];
                constructorThis.updateMouseOverLine(data);

                d3.select(".mouse-line")
                    .attr("d", function () {
                        var d = "M" + mouse[0] + "," + (height + 6);
                        d += " " + mouse[0] + "," + 0;
                        return d;
                    });

                d3.selectAll(".mouse-date")
                    .attr("opacity", "0")
                    .text(scaledX)
                    .attr("y", mouse[0]);
            })
            .on('start', function (d) {
                handleMouseClick(getDateAtMouse(d3.mouse(this), xScale), xScale);

                let dragStartD = d3.selectAll(".caseline").filter(d => d.label === d3.select("text.mouse-date").text()).data()[0];
                constructorThis.updateMouseOverLine(dragStartD);

                d3.selectAll(".mouse-date")
                    .attr("opacity", "0")
            })
            .on('end', function (d) {
                console.log('drag end');
            });

        let mouseG = this.svg.append("g")
            .attr("class", "mouse-over-effects");

        mouseG.append("path") // this is the black vertical line to follow mouse
            .attr("class", "mouse-line")
            .style("stroke", "#7ba4c9")
            .style("stroke-width", "1px")
            .style("shape-rendering", "crispEdges")
            .style("opacity", "0");

        mouseG.append('svg:rect') // append a rect to catch mouse movements on canvas
            .attr('class', 'action-rect')
            .attr('width', width) // can't catch mouse events on a g element
            .attr('height', height)
            .attr('fill', 'none')
            .attr('pointer-events', 'all')
            .style("cursor", "ew-resize")
            .on('mouseout', function () { // on mouse out hide line, circles and text
                d3.select(".mouse-line")
                    .style("opacity", "0");
                d3.selectAll(".mouse-per-line circle")
                    .style("opacity", "0");
                d3.selectAll(".mouse-per-line text")
                    .style("opacity", "0");
                d3.selectAll(".mouse-date")
                    .style("opacity", "0");

            })
            .on('mouseover', function () { // on mouse in show line, circles and text
                d3.select(".mouse-line")
                    .style("opacity", "1");
                d3.selectAll(".mouse-per-line circle")
                    .style("opacity", "1");
                d3.selectAll(".mouse-per-line text")
                    .style("opacity", "1");
                d3.selectAll(".mouse-date")
                    .style("opacity", "1");
            })
            .on('mousemove', function () { // mouse moving over canvas
                let mouse = d3.mouse(this);
                let scaledX = getDateAtMouse(mouse, xScale);
                let opacity = "1";
                if (!scaledX) {
                    scaledX = xScale.domain()[xScale.domain().length - 1];
                }

                if (new Date(scaledX) > new Date(d3.select(".clicked-mouse-date").text())) {
                    opacity = "0";
                }

                //d3.selectAll(".hover-effects").style("opacity", opacity);

                let d = d3.selectAll(".caseline").filter(d => d.label === scaledX).data()[0];
                constructorThis.updateMouseOverLine(d)

                d3.select(".mouse-line")
                    .attr("d", function () {
                        var d = "M" + mouse[0] + "," + (height + 6);
                        d += " " + mouse[0] + "," + 0;
                        return d;
                    });

                d3.selectAll(".mouse-date")
                    .attr("opacity", "1")
                    .text(scaledX)
                    .attr("y", mouse[0]);

            })
            .on('click', function () {
                handleMouseClick(getDateAtMouse(d3.mouse(this), xScale), xScale);

                let d = d3.selectAll(".caseline").filter(d => d.label === d3.select("text.mouse-date").text()).data()[0];
                constructorThis.updateMouseOverLine(d);
            })
            .on('wheel', d => {
                let wheelDelta = d3.event.deltaY;
                let direction = wheelDelta < 0 ? '-1' : '1';
                console.log("Scrolled " + wheelDelta + " " + direction);
                d3.event.preventDefault();
                let selectedDateString = d3.select(".clicked-mouse-date") ? d3.select(".clicked-mouse-date").text() : xScale.domain()[xScale.domain().length - 1];
                console.log("Current scroll date: " + selectedDateString);
                let targetIndex = Math.floor(((xScale(selectedDateString) - xScale("2020-02-17") - (xScale.step() * direction)) / xScale.step()));
                let targetDate = xScale.domain()[targetIndex] ? xScale.domain()[targetIndex] : xScale.domain()[xScale.domain().length - 1];
                console.log("targetDate: " + targetDate);

                if (targetDate) {
                    handleMouseClick(targetDate, xScale);

                    let clickedD = d3.selectAll(".caseline").filter(d => d.label === d3.select("text.clicked-mouse-date").text()).data()[0];
                    let mouseD = d3.selectAll(".caseline").filter(d => d.label === d3.select("text.mouse-date").text()).data()[0];
                    if (!mouseD || new Date(clickedD.label) <= new Date(mouseD.label)) {
                        constructorThis.updateMouseOverLine(clickedD);
                    } else {
                        constructorThis.updateMouseOverLine(mouseD);
                    }
                }
            })
            .call(drag);

        this.svg.select(".action-rect").moveToFront();
    }

    initDateLines() {
        let filteredDates = this.xScale.domain()
            .map(d => d + "T00:00:00")
            .filter(d => new Date(d).getDate().toString().padStart(2, '0') === '01')
            .map(d => d.substring(0, 10));

        console.log("filteredDates: " + filteredDates);

        let enterDates = this.svg.selectAll(".month-lines").data(filteredDates).enter();

        enterDates
            .append("line")
            .attr("class", "month-lines")
            .style("stroke", "white")
            .style("shape-rendering", "crispEdges")
            .style("stroke-width", 1)
            .style("stroke-dasharray", ("3, 3"))
            .attr("x1", d => this.xScale(d) + (this.xScale.bandwidth() / 2))
            .attr("y1", 0)
            .attr("x2", d => this.xScale(d) + (this.xScale.bandwidth() / 2))
            .attr("y2", this.height + 6)
            .attr("opacity", "0.2");

        enterDates
            .append("text")
            .attr("class", "month-text")
            .attr("x", d => -this.height - 7)
            .attr("y", d => this.xScale(d) + this.xScale.bandwidth())
            .text(d => this.monthNames[new Date(d).getMonth() + 1])
            .attr("text-anchor", "end")
            .style("font", "10px sans-serif")
            .style("alignment-baseline", "top")
            .attr("transform", "rotate(-90)");
    }

    /**
     * Transforms the standard epicurve datapoint into a structure to be read by the hover code.
     */
    transformToHoverData(d) {
        if (!d) {
            return [];
        }

        return [{clazz: "reportedcasesbar", color: "#325b8d", label: d.label, y: d.reportedCases, offset: 0},
            {clazz: "caseline", color: "#35a5ff", label: d.label, y: d.cases, offset: 0},
            {clazz: "avgline", color: "#ff7f0e", label: d.label, y: d.movingAvg, offset: 0}]
    }

    /**
     * Handles rendering the hover effects.
     * @param data should be in standard epicurve format.
     */
    updateMouseOverLine(data) {
        if (!data) {
            console.log("UpdateMouseOverLine no data!!!");
            return
        }
        //console.log("UpdateMouseOverLine data: " + data.label);

        const minDistance = 150;
        const thisWidth = this.width;
        const line = d3.line()
            .x(d => d.x)
            .y(d => d.y);

        // Get around scoping issues...
        let blockYscale = this.yScale;
        let blockXscale = this.xScale;

        // Convert the incoming data to the format used by the hover join below.
        let rawHoverData = this.transformToHoverData(data).sort((d1, d2) => {
            if (d1.y < d2.y) {
                return -1;
            }
            if (d1.y > d2.y) {
                return 1;
            }
            return 0;
        });

        // Add offsets to avoid overlapping text
        for (let i = 1; i < rawHoverData.length; i++) {
            let prev = rawHoverData[i - 1];
            let curr = rawHoverData[i];
            let diff = curr.y - prev.y - prev.offset;

            if (diff < minDistance) {
                rawHoverData[i].offset = minDistance - diff;
            }
        }

        // We'll call this in the updateGroup. Builds the path from the hovered point to the axis on the right.
        const buildPath = d => {
            return [{x: blockXscale(d.label) + 7, y: blockYscale(d.y)},              // originating point
                {x: thisWidth, y: blockYscale(d.y)},                                 // axis point
                {x: thisWidth + 6, y: blockYscale(d.y + d.offset)}]     // right-most point with any offset applied for crowding.
        }

        // Dispatch mouseover events for the individual points. Note that we're relying on a custom data attribute selector [isHover="1"]. The event handlers
        // must set this attribute is needed.
        rawHoverData.forEach(hoverD => {
            let unHover = d3.selectAll("." + hoverD.clazz + "[isHover=\"1\"]");
            unHover.dispatch("mouseout");

            let hover = d3.selectAll("." + hoverD.clazz).filter(d => d.label === data.label);
            hover.dispatch("click");
        });

        this.svg.selectAll(".hover-effects")
            .data(rawHoverData, d => d.clazz)
            .join(enter => {
                    // Create group to put the elements under
                    let enterGroup = enter.append("g").attr("class", "hover-effects");

                    // Render y text background
                    enterGroup.append("svg:rect")
                        .attr("class", d => d.clazz + "-hover")
                        .attr("x", this.width + 8)
                        .attr("y", d => blockYscale(d.y) - 6)
                        .attr("width", 45)
                        .attr("height", 12)
                        .style("fill", "#103052")
                        .attr("opacity", "1");

                    // Render line
                    enterGroup.append("path")
                        .attr("class", d => d.clazz + "-hover")
                        .style("stroke", d => d.color)
                        .style("shape-rendering", "crispEdges")
                        .style("stroke-width", 1)
                        .style("fill", "none")
                        .attr("d", d => line(buildPath(d)))
                        .attr("opacity", "1");

                    // Render text
                    enterGroup.append("text")
                        .attr("class", d => d.clazz + "-hover")
                        .attr("x", this.width + 17)
                        .attr("alignment-baseline", "start")
                        .attr("y", d => blockYscale(d.y) + 9)
                        .attr("dx", "-.8em")
                        .attr("dy", "-.55em")
                        .style("fill", d => d.color)
                        .style("font", "10px sans-serif")
                        .text(d => d.y)
                        .attr("opacity", "1");
                },
                updateGroup => {
                    // Update positions
                    updateGroup.select("rect").attr("y", d => blockYscale(d.y + d.offset) - 6);

                    updateGroup.select("path").attr("d", d => line(buildPath(d)));

                    updateGroup.select("text")
                        .attr("y", d => blockYscale(d.y + d.offset) + 9)
                        .text(d => d.y);
                });
    }

    createAxisLabels(xLabel, yLabel) {
        this.svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(this.xAxis)
            .selectAll("text")
            .style("text-anchor", "end")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("transform", "rotate(-90)");

        this.svg.append("text")
            .attr("class", "x label")
            .attr("text-anchor", "end")
            .attr("x", this.width)
            .attr("y", this.height - 6)
            .text(xLabel);

        this.svg.append("g")
            .attr("class", "y axis")
            .call(this.yAxis)
            .append("text")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(yLabel)
            .attr("transform", "translate(5,5)rotate(-90)");
        //.attr("transform", "translate(5,5)rotate(-90)");
    }

    updateBarChart(data, xCallback, yCallback, clazz, color, highlightColor) {
        let selectedData = this.svg.selectAll("." + clazz).data(data);
        let enterData = selectedData.enter();

        selectedData.exit().remove();

        enterData.append("rect")
            .style("shape-rendering", "crispEdges")
            .attr("class", clazz)
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
            .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
            .attr("width", this.xScale.bandwidth())
            .style("fill", d => yCallback(d) > 0 ? color : highlightColor);

        selectedData.transition().duration(90)
            .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
            .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
            .style("fill", d => yCallback(d) > 0 ? color : highlightColor);
    }

    updateLineChartY1(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded) {
        this.updateLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, this.yScale)
    }

    updateLineChartY2(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded) {
        this.updateLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, this.yScale2)
    }

    updateLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, yScale) {
        let dYScale = yScale ? yScale : this.yScale;
        let selectedData = this.svg.selectAll("." + clazz).data([data], d => xCallback(d));
        let thisXScale = this.xScale;

        // Draw initial line
        selectedData.enter()
            .append("path")
            .attr("class", clazz)
            .attr("d", d3.line()
                .x(d => this.getLineX(xCallback(d), isBanded))
                .y(d => this.getLineY(yCallback(d), dYScale))
                // Don't draw the line if it's in the preliminary region.
                .defined(d => this.isPointDefined(yCallback, d, prelimRegionStart)))
            .attr("fill", "none")
            .attr("stroke", color)
            .attr("stroke-width", 1);

        // Update line
        selectedData
            .merge(selectedData)
            .transition()
            .duration(90)
            .attr("d", d3.line()
                .x(d => this.getLineX(xCallback(d), isBanded))
                .y(d => this.getLineY(yCallback(d), dYScale))
                // Don't draw the line if it's in the preliminary region.
                .defined(d => this.isPointDefined(yCallback, d, prelimRegionStart)));

        var circleClass = clazz + '-circle';
        var isVisible = "visible" === this.svg.selectAll('.' + clazz).style("visibility");
        var selectedCircles = this.svg.selectAll('.' + circleClass).data(data);

        // Removed data
        selectedCircles.exit().transition().duration(90).style("opacity", 0).remove();

        // Initial data
        selectedCircles.enter()
            .append('circle')
            .attr('class', circleClass + " " + clazz)
            .attr('cx', d => this.getLineX(xCallback(d), isBanded))
            .attr('cy', d => this.getLineY(yCallback(d), dYScale))
            .attr("stroke", highlightColor)
            .attr("fill", color)
            .attr("opacity", d => this.getLineOpacity(yCallback, d, prelimRegionStart))
            .attr('r', 2)
            .style("visibility", d => isVisible ? "visible" : "hidden")
            .on('click', function (d, i) {
                d3.select(this).attr("r", 5).attr("isHover", "1");
            })
            .on('mouseout', function (d, i) {
                d3.select(this).attr("r", 2).attr("isHover", "0");
            });


        // Updated data
        selectedCircles
            .merge(selectedCircles)
            .transition()
            .duration(90)
            .attr("opacity", d => this.getLineOpacity(yCallback, d, prelimRegionStart))
            .attr('cx', d => this.getLineX(xCallback(d), isBanded))
            .attr('cy', d => this.getLineY(yCallback(d), dYScale));
    }

    updateFloatingPoints(data, xCallback, yCallback, clazz, color) {
        let filteredData = data.filter(d => !isNaN(yCallback(d)));
        let thisXScale = this.xScale;
        let thisYScale = this.yScale;

        filteredData.forEach(d => {
            d.x = xCallback(d);
            d.y = yCallback(d);
            d.scaledX = this.getLineX(xCallback(d), true);
            d.scaledY = this.getLineY(yCallback(d), this.yScale);
            d.color = color;
        });

        let selectedData = this.svg.selectAll("." + clazz).data(filteredData);

        let triangle = d3.symbol()
            .size(20)
            .type(d3.symbolDiamond);

        selectedData.exit().transition().duration(90).style("opacity", 0).remove();

        let isVisible = true;

        if (this.svg.selectAll('.' + clazz).data().length > 0) {
            isVisible = "visible" === this.svg.selectAll('.' + clazz).style("visibility");
        }

        selectedData.enter().append("path")
            .attr("class", clazz)
            .style("visibility", isVisible ? "visible" : "hidden")
            .attr("d", triangle)
            .attr("stroke", color)
            .attr("fill", color)
            .attr("opacity", "0.5")
            .attr("transform", d => "translate(" + d.scaledX + "," + d.scaledY + ")")
            .on('click', function (d) {
                d3.select(this)
                    .attr("opacity", "1")
                    .attr("isHover", "1")
                    .attr("d", d3.symbol().size(100).type(d3.symbolDiamond));
            })
            .on('mouseout', function (d, i) {
                d3.select(this)
                    .attr("opacity", "0.5")
                    .attr("isHover", "0")
                    .attr("d", d3.symbol().size(20).type(d3.symbolDiamond));
            });

        selectedData
            .merge(selectedData)
            .transition()
            .duration(90);
    }

    updateCorrelationLine(correlationCoefficient, clazz, color) {
        let rounded = Math.round((correlationCoefficient + Number.EPSILON) * 10000) / 10000

        let selectedLine = this.svg.selectAll('.' + clazz).data([rounded]);

        let top = 80;
        let mid = top / 2;
        let right = 3500;

        selectedLine.enter()
            .append("line")
            .attr("class", clazz)
            .style("stroke", color)
            .style("stroke-width", 2)
            .attr("x1", d => this.xScale(0))
            .attr("y1", d => this.yScale(mid - (mid * d)))
            .attr("x2", d => this.xScale(right))
            .attr("y2", d => this.yScale((mid * d) + mid));

        selectedLine.merge(selectedLine)
            .transition()
            .duration(90)
            .attr("y1", d => this.yScale(mid - (mid * d)))
            .attr("y2", d => this.yScale((mid * d) + mid));

        let textClass = clazz + '-text';
        let selectedText = this.svg.selectAll('.' + textClass).data([rounded]);

        selectedText.enter()
            .append("text")
            .attr("class", textClass)
            .attr("x", d => this.xScale(right - 130))
            .attr("y", d => this.yScale((mid * d) + mid) - 10)
            .text(d => "r: " + d)
            .attr("text-anchor", "top")
            .style("alignment-baseline", "top")
            .style("fill", color)
            .style("font", "12px \"Courier New\", Courier, monospace")
        //.attr("transform", "rotate(-90)")

        selectedText
            .merge(selectedText)
            .transition()
            .duration(90)
            .text(d => "r: " + d)
            .attr("y", d => this.yScale((mid * d) + mid) - 10);


        selectedText.exit().remove();
    }

    getLineX(d, isBanded) {

        if (isBanded) {
            return this.xScale(d) + (this.xScale.bandwidth() / 2);
        }

        return this.xScale(d);
    }

    /**
     * Convenience function to set opacity to 0 for NaN, and 0.5 for preliminary region.
     */
    getLineOpacity(dataCallback, dataPoint, prelimRegionStart) {
        if (isNaN(dataCallback(dataPoint))) {
            return "0";
        }

        if (prelimRegionStart && this.xScale(dataPoint.label) >= this.xScale(prelimRegionStart)) {
            return "0.5";
        }

        return "1";
    }

    /**
     * Used to determine if a data point should be shown on the graph. NaN's should not be shown, and neither should points
     * after the given 'preliminary' region start date.
     *
     * @param dataCallback The callback to get the Y value
     * @param dataPoint The data point object which contains all relevant information for the given point.
     * @param prelimRegionStart The start of the preliminary region. Points after this should not be shown.
     * @returns true if the point should be shown on the graph, false if the point should not be shown.
     */
    isPointDefined(dataCallback, dataPoint, prelimRegionStart) {
        if (isNaN(dataCallback(dataPoint))) {
            return false;
        }

        if (prelimRegionStart) {
            return this.xScale(dataPoint.label) < this.xScale(prelimRegionStart);
        }

        return true;
    }

    /**
     * Convenience function to check for NaN's.
     */
    getLineY(d, yScale) {
        if (!isNaN(d)) {
            return yScale(d);
        }

        return 0;
    }

    drawMouseOverRects(xCallback, data) {
        var selectedData = this.svg.selectAll(".mouseoverclazz").data(data);
        var enterData = selectedData.enter();

        let rects = enterData.append("rect")
            .attr("class", "mouseoverclazz")
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", d => 0)
            .attr("height", d => this.height)
            .attr("width", this.xScale.bandwidth())
            .style("fill", "#999")
            .attr("opacity", "0")
            .on("mouseover", function (d, i) {
                handleDeathMouseOver(d, i, d3.select(this), "#fff")
            })
            .on("mouseout", function (d, i) {
                handleDeathMouseOut(d, i, d3.select(this), "#999")
            });

        selectedData.exit().remove();

        return rects;
    }

    draw14DayWindow(xCallback, offsetDate, offset) {
        let selectedData = this.svg.selectAll(".prelim-region").data([offsetDate]);

        // update region location
        selectedData
            .merge(selectedData)
            .attr("width", this.xScale.bandwidth() * (offset + 1))
            .attr("x", d => this.xScale(d))
            .transition()
            .attr("opacity", "0.05");

        let boundaryLineSelectedData = this.svg.selectAll(".prelim-boundary-line").data([offsetDate]);
        let boundaryLineEnterData = boundaryLineSelectedData.enter();

        // draw initial boundary line
        boundaryLineEnterData
            .append("line")
            .attr("class", "prelim-boundary-line")
            .style("stroke", "#7ba4c9")
            .style("shape-rendering", "crispEdges")
            .style("stroke-width", 1)
            .attr("x1", d => this.xScale(d))
            .attr("y1", 0)
            .attr("x2", d => this.xScale(d))
            .attr("y2", this.height);

        // update boundary line location
        boundaryLineSelectedData
            .merge(boundaryLineSelectedData)
            .attr("x1", d => this.xScale(d))
            .attr("x2", d => this.xScale(d));

        // draw initial text
        let textSelectedData = this.svg.selectAll(".prelim-text").data([{date: offsetDate, label: "Preliminary Data"}]);
        let textEnterData = textSelectedData.enter();

        textEnterData
            .append("text")
            .attr("class", "prelim-text")
            .attr("x", d => -80)
            .attr("y", d => this.xScale(d.date) + this.xScale.bandwidth() + 5)
            .text(d => xCallback(d))
            .attr("text-anchor", "top")
            .style("alignment-baseline", "top")
            .attr("transform", "rotate(-90)")

        textSelectedData
            .merge(textSelectedData)
            .attr("y", d => this.xScale(d.date) + this.xScale.bandwidth() + 5);
    }

    applyDateOffset(date, offset) {
        let dateObj = this.convertDate(date);
        dateObj.setDate(dateObj.getDate() - offset);
        return this.getFormattedDate(dateObj);
    }

    getFormattedDate(date) {
        let year = date.getFullYear();
        let month = (1 + date.getMonth()).toString().padStart(2, '0');
        let day = date.getDate().toString().padStart(2, '0');

        return year + '-' + month + '-' + day;
    }

    /**
     * Convert a date string in 2020-10-10T101300 format into a Date() object.
     */
    convertDate(dateString) {
        let filteredDateString = dateString.slice(0, 15) + ":" + dateString.slice(15 + Math.abs(0))
        filteredDateString = filteredDateString.slice(0, 13) + ":" + filteredDateString.slice(13 + Math.abs(0));
        filteredDateString = filteredDateString.replace("T", " ");
        return new Date(filteredDateString);
    }

    /**
     * Parses the provided incoming csv files into a more useable JSON document, indexed by report ID (ex: 2020-05-27T09:00:03)
     *
     * @param summaryData The summary csv
     * @param caseData The case csv
     * @param deathData The death csv
     * @param caseDeltaData The case delta csv
     * @param deathDeltaData The death delta csv
     * @param caseProjectionData The case projection csv
     * @param movingAvgData The moving averages csv
     * @returns [{
     *     cases: 2,
     *     deaths: 0,
     *     label: "2020-02-17"
     * },{
     *     cases: 36,
     *     deaths: 0,
     *     label: "2020-05-27"
     * }]
     */
    parseChartData(summaryData, caseData, deathData, caseDeltaData, deathDeltaData, caseProjectionData, movingAvgData, histogramData) {
        let tempChartData = [];

        let reportedCasesData = []
        summaryData.forEach(function (d) {
            reportedCasesData[d.reportDate] = d.confirmedCasesVm;
        });

        let reportedDeathsData = []
        summaryData.forEach(function (d) {
            reportedDeathsData[d.reportDate] = d.deathsVm;
        });

        for (let i = 0; i < caseData.length; i++) {
            let currentSummaryData = summaryData[i];
            let currentCaseData = caseData[i];
            let currentDeathData = deathData[i];
            let currentCaseDeltaData = caseDeltaData[i];
            let currentDeathDeltaData = deathDeltaData[i];
            //let currentCaseProjectionData = caseProjectionData[i];
            let currentMovingAvgData = movingAvgData[i];
            let currentReportDates = Object.keys(currentCaseData).filter(function (d) {
                return d !== 'id'
            });
            let currentTimeseries = tempChartData[currentCaseData.id];
            if (!currentTimeseries) {
                currentTimeseries = [];
                tempChartData[currentCaseData.id] = currentTimeseries;
            }
            currentReportDates
                .filter(function (d) {
                    return !!currentCaseData[d];
                })
                .forEach(function (d) {
                    currentTimeseries.push({
                        label: d,
                        cases: +currentCaseData[d],
                        deaths: +currentDeathData[d],
                        casesDelta: +currentCaseDeltaData[d],
                        deathsDelta: +currentDeathDeltaData[d],
                        //casesProjection: +currentCaseProjectionData[d],
                        movingAvg: +currentMovingAvgData[d],
                        reportedCases: +reportedCasesData[d],
                        reportedDeaths: +reportedDeathsData[d]
                    });
                });
        }

        return tempChartData;
    }

    parseCorrelationData(summaryData, caseData, deathData, deathOffset) {
        let tempChartData = [];

        let reportedCasesData = []
        let filteredSummaryData = summaryData.filter(d => d.reportDate >= "2020-05-11")

        filteredSummaryData.forEach(function (d) {
            reportedCasesData[d.reportDate] = d.confirmedCasesVm;
        });

        let reportedDeathsData = []
        filteredSummaryData.forEach(function (d) {
            reportedDeathsData[d.reportDate] = d.deathsVm;
        });

        let dateIndexLookup = {};
        let indexDateLookup = {};
        let allReportDates = Object.keys(deathData[deathData.length - 1]).filter(d => d !== 'id').filter(d => d >= "2020-05-11");
        for (let i = 0; i < allReportDates.length; i++) {
            let currentDate = allReportDates[i];
            dateIndexLookup[currentDate] = i;
            indexDateLookup[i] = currentDate;
        }

        for (let i = 0; i < filteredSummaryData.length; i++) {
            let currentSummaryData = filteredSummaryData[i];
            let currentCaseData = caseData[i];
            let currentDeathData = deathData[i];
            let currentReportDates = Object.keys(currentCaseData).filter(d => d !== 'id').filter(d => d >= "2020-05-11");
            let currentTimeseries = tempChartData[currentSummaryData.id];
            if (!currentTimeseries) {
                currentTimeseries = [];
                tempChartData[currentSummaryData.id] = currentTimeseries;
            }
            currentReportDates
                .filter(d => {
                    return !!currentCaseData[d];
                })
                .forEach(d => {
                    let deathOffsetDate = indexDateLookup[dateIndexLookup[d] + deathOffset];
                    if (deathOffsetDate && currentDeathData[deathOffsetDate]) {
                        currentTimeseries.push({
                            label: d,
                            cases: +currentCaseData[d],
                            deaths: +currentDeathData[deathOffsetDate],
                            casesAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, currentCaseData, d, 7),
                            deathsAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, currentDeathData, deathOffsetDate, 7),
                            reportedCases: +reportedCasesData[d] || 0,
                            reportedDeaths: +reportedDeathsData[deathOffsetDate] || 0,
                            reportedCasesAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, reportedCasesData, d, 7),
                            reportedDeathsAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, reportedDeathsData, deathOffsetDate, 7),
                        });
                    }
                });
        }

        return tempChartData;
    }

    calcMovingAverage(indexDateLookup, dateIndexLookup, data, currentDate, window) {
        let currentDateIdx = dateIndexLookup[currentDate];

        let sum = 0;
        for (let i = 0; i < 7; i++) {
            sum += +data[indexDateLookup[currentDateIdx - i]] || 0;
        }

        return sum / window;
    }

    make_y_gridlines() {
        return d3.axisLeft(this.yScale).tickValues([0, 100, 200, 300, 400, 500, 600, 700, 800, 900])
    }

    drawAxisLines() {
        this.svg.selectAll(".grid").remove();
        this.svg.append("g")
            .attr("class", "grid")
            .call(this.make_y_gridlines()
                .tickSize(-width)
                .tickFormat("")
            );
    }

    createXAxisTickValues(data) {
        let lastElement = getLastElement(data);
        let xAxisTickValues = [];

        /*        for (let i = 0; i < lastElement.length; i += 7) {
                    xAxisTickValues.push(lastElement[i].label);
                }*/

        return xAxisTickValues;
    }

    createAxis(data, yLabel, yLabel2) {
        this.createBottomAxis(data);

        this.svg.append("g")
            .attr("class", "y axis")
            .call(this.yAxis)
            .append("text")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(yLabel)
            .attr("transform", "translate(5,5)rotate(-90)");

        if (this.yScale2 && this.yAxis2) {
            this.createRightSideAxis(yLabel2, false);
        }
    }

    createBottomAxis(data) {
        this.svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(this.xAxis.tickValues(this.createXAxisTickValues(data)))
            .selectAll("text")
            .style("text-anchor", "end")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("transform", "rotate(-90)");
    }

    createRightSideAxis(label, leftlabel) {
        let translate = leftlabel ? "translate(-8,5)rotate(-90)" : "translate(5,5)rotate(-90)";

        this.svg.append("g")
            //.attr("transform", "translate("+(this.width-4)+",0)")
            .attr("transform", "translate(" + (this.width) + ",0)")
            .attr("class", "y axis")
            .call(this.yAxis2 ? this.yAxis2 : this.yAxis)
            .append("text")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(label)
            .attr("transform", translate);
    }

    getYScaleMax(data) {
        if (deltaEnabled) {
            return 300;
        }

        return 50 + d3.max(data, d => d.cases)
    }

    getYScaleMin(data) {
        let minDelta = d3.min(data, d => d.casesDelta);
        let maxDelta = d3.max(data, d => d.casesDelta);

        if (minDelta > -10 && maxDelta < 100) {
            return -10;
        }

        if (minDelta <= -10 || maxDelta >= 100) {
            return -100
        }

        return -100;
    }

    createTooltips() {
        tip = d3.tip().attr('class', 'd3-tip').direction('e').offset([0, 5])
            .html(function (d) {
                var content = "<span style='margin-left: 2.5px;'><b>" + d.label + "</b></span><br>";
                content += `
                    <table style="margin-top: 2.5px;">
                            <tr><td>Reported Cases: </td><td style="text-align: right">` + getReportedCases(d) + `</td></tr>
                            <tr><td>Reported Deaths: </td><td style="text-align: right">` + getReportedDeaths(d) + `</td></tr>
                            <tr><td>Confirmed Cases: </td><td style="text-align: right">` + getCases(d) + `</td></tr>
                            <tr><td>Confirmed Deaths: </td><td style="text-align: right">` + getDeaths(d) + `</td></tr>
                            <tr><td>Case Delta: </td><td style="text-align: right">` + getCaseDeltas(d) + `</td></tr>
                            <tr><td>Death Delta: </td><td style="text-align: right">` + getDeathDeltas(d) + `</td></tr>
                            <tr><td>Case 7d Avg: </td><td style="text-align: right">` + getMovingAvg(d) + `</td></tr>
                    </table>
                    `;
                return content;
            });
        this.svg.call(tip);
    }

    createLineLegend(legend, clickCallback) {
        let size = 10;

        this.svg.selectAll(".legend-lines")
            .data(legend)
            .enter()
            .append("line")
            .on("click", clickCallback)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("class", "legend-lines")
            .style("stroke", d => d.color)
            .style("shape-rendering", "crispEdges")
            .style("stroke-width", 1)
            .attr("x1", d => d.x - 5)
            .attr("y1", (d, i) => this.height + 85 + (i * (size + 5)))
            .attr("x2", d => d.x - 5 + size)
            .attr("y2", (d, i) => this.height + 85 + (i * (size + 5)));

        let circleX = [];
        legend.forEach(function (d, i) {
            circleX.push({yOffset: i, x: d.x, color: d.color});
            circleX.push({yOffset: i, x: d.x + size, color: d.color});
        });

        this.svg.selectAll(".legend-circles")
            .data(circleX)
            .enter()
            .append('circle')
            .on("click", clickCallback)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr('class', "legend-circles")
            .attr('cx', d => d.x - 5)
            .attr('cy', d => this.height + 85 + (d.yOffset * (size + 5)))
            //.attr("stroke", highlightColor)
            .style("fill", d => d.color)
            .attr('r', 2);

        this.svg.selectAll("mylabels")
            .data(legend)
            .enter()
            .append("text")
            .on("click", clickCallback)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x + size + 5)
            .attr("y", (d, i) => this.height + 80 + (i * (size + 5)) + 9)
            .text(d => d.key)
            .attr("text-anchor", "left")
            .style("fill", d => d.color)
            .style("font", "11px sans-serif")
            .style("alignment-baseline", "middle")
    }

    createBoxLegend(legend) {
        let size = 8
        this.svg.selectAll("mydots")
            .data(legend)
            .enter()
            .append("rect")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x)
            .attr("y", (d, i) => getLegendHeight(this.height, size, i))
            .attr("width", size)
            .attr("height", size)
            .style("fill", d => d.color)


        this.svg.selectAll("mylabels")
            .data(legend)
            .enter()
            .append("text")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x + size + 5)
            .attr("y", (d, i) => this.height + 80 + (i * (size + 7)) + 9)
            .style("fill", d => d.textColor ? d.textColor : d.color)
            .text(d => d.key)
            .attr("text-anchor", "left")
            .style("font", "11px sans-serif")
            .style("alignment-baseline", "middle")
    }

    createShapeLegend(legend) {
        let size = 8;
        let triangle = d3.symbol()
            .type(d3.symbolDiamond)
            .size(20);

        this.svg.selectAll("myshapes")
            .data(legend)
            .enter()
            .append("path")
            //.attr("class", clazz)
            .attr("d", triangle)
            .attr("transform", (d, i) => "translate(" + (d.x + 5) + "," + (getLegendHeight(this.height, size, i) + 4) + ")")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .style("fill", d => d.color)
            .style("stroke", d => d.color);


        this.svg.selectAll("mylabels")
            .data(legend)
            .enter()
            .append("text")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x + size + 5)
            .attr("y", (d, i) => this.height + 80 + (i * (size + 7)) + 9)
            .style("fill", d => d.textColor ? d.textColor : d.color)
            .text(d => d.key)
            .attr("text-anchor", "left")
            .style("font", "11px sans-serif")
            .style("alignment-baseline", "middle")
    }

    calculateCorrelationCoefficient(xData, yData) {
        if (xData.length !== yData.length) {
            throw "In order to calculate standard deviation, x and y must have the same number of elements!";
        }

        let NaNCount = 0;
        for (let i = 0; i < xData.length; i++) {
            if (!isNaN(xData[i]) && !isNaN(yData[i])) {
                break;
            }

            if (isNaN(xData[i]) || isNaN(yData[i])) {
                NaNCount++;
            }
        }

        //console.log("xData.length: " + xData.length + ", xData: " + xData);
        //console.log("yData.length: " + yData.length + ", yData: " + yData);
        //console.log(NaNCount + " NaN's found at the beginning of the array! yData.length: " + yData.length);
        for (let i = 0; i < NaNCount; i++) {
            xData.pop();
            yData.shift();
        }
        //console.log("After shift - xData.length: " + xData.length + ", yData.length: " + yData.length);
        //console.log("xData.length: " + xData.length + ", xData: " + xData);
        //console.log("yData.length: " + yData.length + ", yData: " + yData);

        let xMean = xData.reduce((a, b) => a + b) / xData.length;
        console.log("xMean: " + xMean);
        let yMean = yData.reduce((a, b) => a + b) / yData.length;
        console.log("yMean: " + yMean);
        let xStdev = Math.sqrt(xData.reduce(function (sq, n) {
            return sq + Math.pow(n - xMean, 2);
        }, 0) / (xData.length - 1));
        console.log("xStdev: " + xStdev);
        let yStdev = Math.sqrt(yData.reduce(function (sq, n) {
            return sq + Math.pow(n - yMean, 2);
        }, 0) / (yData.length - 1));
        console.log("yStdev: " + yStdev);
        let xStandardized = xData.map(function (d) {
            return (d - xMean) / xStdev;
        });
        //console.log("xStandardized: " + xStandardized);
        let yStandardized = yData.map(function (d) {
            return (d - yMean) / yStdev;
        });
        //console.log("yStandardized: " + yStandardized);
        let multipliedCoords = [];

        for (let i = 0; i < xStandardized.length; i++) {
            multipliedCoords.push(xStandardized[i] * yStandardized[i]);
        }

        let correlationCoefficient = multipliedCoords.reduce((a, b) => a + b) / (xData.length - 1);

        console.log("Correlation Coefficient: " + correlationCoefficient);

        return correlationCoefficient;
    }
}

function createSlider(data) {
    let count = 0;

    for (let value of Object.values(data)) {
        if (value.length > 0) {
            count++
        }
    }

    count = (count - 1) * -1;

    let tickValueArray = [];

    for (let current = 0; current > count; current = current - 7) {
        tickValueArray.push(current);
    }

    let sliderTime = d3
        .sliderBottom()
        //.min(-1 * (timeData.length - 1))
        //.max(0)
        .domain([count, 0])
        .marks([count, 0])
        .step(1)
        //.ticks(5)
        .width(350)
        .tickValues(tickValueArray);

    var gTime = d3
        .select('div#slider-time')
        .append('svg')
        .attr('width', 400)
        .attr('height', 100)
        .append('g')
        .attr('transform', 'translate(30,30)');

    gTime.call(sliderTime);

    return sliderTime;
}

function getLastElement(data) {
    let keySet = Object.keys(data);
    return data[keySet[keySet.length - 1]];
}

/**
 * Gets the value from the provided array as if it were indexed from the end. So 0 gets the last element, and -data.length-1 gets the first element.
 * -1 Gets the next to the last element.
 */
function getReverseIdxValue(data, idx) {
    return data[data.length - 1 + idx];
}

function getLegendHeight(height, size, i) {
    return height + 81 + (i * ((size + 2) + 5))
}

function toggleVisibilityExclusive(d) {
    toggleVisibility(d, true);
}

function toggleVisibility(d, exclusive) {
    d.clazz.forEach(c => {
        let currentVis = d3.selectAll("." + c).style("visibility");
        console.log("Legend click! Toggling " + d.key + "... current visibility: " + currentVis);

        let targetVis = "visible";
        if (targetVis === currentVis) {
            targetVis = "hidden";
        }

        d3.selectAll("." + c).transition().duration(200).style("visibility", targetVis);
    });
}

function handleDeathMouseOver(d, i, d3This, color) {
    d3This
        .transition()
        .duration(50)
        .style("fill", color)
        .attr("opacity", "0.2");
    tip.show(d, i);
}

function handleDeathMouseOut(d, i, d3This, color) {
    d3This
        .transition()
        .duration(200)
        .style("fill", color)
        .attr("opacity", "0");
    tip.hide(d, i);
}

function dispatchMouseEvents(clazz, scaledX, prevNode, currentNode) {
    currentNode[clazz] = d3.selectAll("." + clazz).filter(d => d.label === scaledX);

    // First time entering?
    if (!prevNode[clazz] && currentNode[clazz]) {
        //console.log("First time? scaledX: " + scaledX);
        prevNode[clazz] = currentNode[clazz];
        currentNode[clazz].dispatch("click");
    }

    // Exited past the range of available data
    if (!currentNode[clazz].data()[0] || currentNode[clazz].data().length === 0) {
        //console.log("Exit past available? prevLabel: " + prevNode[clazz].data()[0].label + " scaledX: " + scaledX);
        prevNode[clazz].dispatch("mouseout");
    }

    // Most common case, moving from 1 good date to the next.
    if ((prevNode[clazz].data().length > 0 && currentNode[clazz].data().length) && (prevNode[clazz].data()[0].label !== currentNode[clazz].data()[0].label)) {
        //console.log("Good transition? prevLabel: " + prevNode[clazz].data()[0].label + " currLabel: " + currentNode[clazz].data()[0].label + " scaledX: " + scaledX);
        prevNode[clazz].dispatch("mouseout");
        currentNode[clazz].dispatch("click");
        prevNode[clazz] = currentNode[clazz];
    } else {
        //console.log("Doing nothing! prevLabel: " + prevNode[clazz].data()[0].label + " scaledX: " + scaledX);
    }
}

function sumTimeSeries(data) {
    return Object.values(data).map(d => +d).filter(d => !isNaN(d)).reduce((d1, d2) => d1 + d2)
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

function getFormattedDate(date) {
    let year = date.getFullYear();
    let month = (1 + date.getMonth()).toString().padStart(2, '0');
    let day = date.getDate().toString().padStart(2, '0');

    return year + '-' + month + '-' + day;
}

const earliestDate = new Date("2020-05-14");

function getDateAtMouse(mouse, xScale) {
    let index = Math.floor(((mouse[0] - xScale("2020-02-17")) / xScale.step()));
    let scaledX = xScale.domain()[index];
    if (!scaledX) {
        scaledX = xScale.domain()[xScale.domain().length - 1];
    }

    if (new Date(scaledX) < this.earliestDate) {
        scaledX = getFormattedDate(this.earliestDate);
    }

    //console.log("scaledX " + scaledX + " @ mouse " + mouse[0]);

    return scaledX;
}

function handleMouseClick(scaledX, xScale) {
    let event = new CustomEvent('newDateEvent', {detail: {label: scaledX}});

    dispatchEvent(event);

    d3.selectAll(".clicked-mouse-date")
        .attr("opacity", "1")
        .text(scaledX)
        .attr("y", xScale(scaledX) + (xScale.step() / 2));

    d3.select(".clicked-mouse-date-line")
        .attr("opacity", "1")
        .attr("x1", xScale(scaledX) + (xScale.step() / 2))
        .attr("x2", xScale(scaledX) + (xScale.step() / 2));

    //d3.selectAll(".hover-effects").style("opacity", "0");
}

d3.selection.prototype.moveToFront = function () {
    return this.each(function () {
        this.parentNode.appendChild(this);
    });
};