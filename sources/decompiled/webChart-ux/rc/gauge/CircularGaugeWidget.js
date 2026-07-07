/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/gauge/CircularGaugeWidget
 */
define(['bajaux/Widget', 'bajaux/mixin/subscriberMixIn', 'bajaux/dragdrop/dragDropUtils', 'd3', 'baja!', 'Promise', 'nmodule/js/rc/switchboard/switchboard', 'baja!baja:StatusValue,baja:IEnum,control:ControlPoint', 'nmodule/webChart/rc/gauge/model', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/webChartUtil', 'css!nmodule/webChart/rc/gauge/CircularGaugeWidgetStyle'], function (Widget, subscriberMixIn, dragDropUtils, d3, baja, Promise, switchboard, types, model, modelUtil, webChartUtil) {
  "use strict";

  var options = {
    left: 5,
    top: 5,
    minAngle: -125,
    maxAngle: 125,
    ringInset: 30,
    ringWidth: 10,
    pointerHeadLengthPercent: 0.8,
    labelInset: 20,
    pointerWidth: 10,
    pointerTailLength: 5,
    transitionMs: 750,
    pointerLine: d3.svg.line().interpolate('monotone'),
    valueTextYOffset: 30,
    titleYOffset: 60,
    defaultShapeColor: "#c1c1c1"
  },
      overrideOrdName = "overrideOrd";

  function widgetDefaults() {
    return {
      properties: {
        title: '%displayName%',
        valueText: '%out.value%',
        ticks: 5,
        min: -1,
        max: -1,
        overrideOrd: {
          value: '',
          hidden: true,
          readonly: true,
          dashboard: true
        }
      }
    };
  }
  /**
   * An HTML5 Circular Gauge.
   *
   * @class
   * @alias module:nmodule/webChart/rc/gauge/CircularGaugeWidget
   * @extends module:bajaux/Widget
   */


  var CircularGaugeWidget = function CircularGaugeWidget(params) {
    var that = this;
    Widget.call(that, {
      params: params,
      defaults: widgetDefaults()
    });
    switchboard(this, {
      '$renderData': {
        allow: 'oneAtATime',
        onRepeat: 'preempt'
      }
    });
    subscriberMixIn(that);
  };

  CircularGaugeWidget.prototype = Object.create(Widget.prototype);
  CircularGaugeWidget.prototype.constructor = CircularGaugeWidget; ////////////////////////////////////////////////////////////////
  // Render
  ////////////////////////////////////////////////////////////////

  /**
   * Return the size of the Widget.
   *
   * @inner
   * @private
   *
   * @param  widget The widget instance.
   * @returns {Object} The size object with both width and height.
   */

  function getSize(widget) {
    var jq = widget.jq();
    return {
      width: jq.width(),
      height: jq.height()
    };
  }
  /**
   * Convert degrees to radians.
   *
   * @private
   * @inner
   *
   * @param  {Number} deg The number of degrees.
   * @returns {Number} The calculated radians.
   */


  function degToRad(deg) {
    return deg * Math.PI / 180;
  }
  /**
   * Render the gauge.
   *
   * @private
   * @inner
   *
   * @param widget The Widget instance.
   * @param {Object} data The data to render.
   * @returns {Promise}
   */


  function renderData(widget, data) {
    var dataArray = [data],
        size = getSize(widget),
        svg = d3.select(widget.jq()[0]).select(".CircularGaugeWidget"),
        gauge = svg.select("g"),
        range = options.maxAngle - options.minAngle,
        promises = [],
        scale;
    /* if changing, update CSS as well. */

    data.width = size.width || 300;
    data.height = size.height || 260;
    data.radius = data.width / 2;
    data.centerTx = 'translate(' + data.radius + ',' + data.radius + ')';
    scale = d3.scale.linear().range([0, 1]).domain([data.min, data.max]);

    (function renderArc() {
      var arc, arcs, path, labels, ticks, tickData, lines, text; // Ticks and Scale

      ticks = scale.ticks(data.ticks);
      tickData = d3.range(data.ticks).map(function () {
        return 1 / data.ticks;
      });
      arc = d3.svg.arc().innerRadius(data.radius - options.ringWidth - options.ringInset).outerRadius(data.radius - options.ringInset).startAngle(function (d, i) {
        var ratio = d * i;
        return degToRad(options.minAngle + ratio * range);
      }).endAngle(function (d, i) {
        var ratio = d * (i + 1);
        return degToRad(options.minAngle + ratio * range);
      }); // Arcs

      arcs = gauge.selectAll('.arc').data(dataArray);
      arcs.enter().append('g').attr('class', 'arc');
      arcs.attr('transform', function (d) {
        return d.centerTx;
      });
      arcs.exit().remove(); // Arc Path

      path = arcs.selectAll('path').data(tickData);
      path.enter().append('path').attr("class", "arcPath").attr("fill", options.defaultShapeColor);
      path.attr('d', arc).transition().attr("fill", data.color ? data.color : options.defaultShapeColor);
      path.exit().remove(); // Labels

      labels = gauge.selectAll('.label').data(dataArray);
      labels.enter().append('g').attr('class', 'label');
      labels.attr('transform', data.centerTx);
      labels.exit().remove(); // Extra tick marks

      lines = labels.selectAll('line').data(ticks);
      lines.enter().append('line').attr('class', 'tickline').attr('x1', 0).attr('y1', 0).attr('x2', 0).attr('y2', options.ringWidth);
      lines.attr('transform', function (d) {
        var ratio = scale(d),
            newAngle = options.minAngle + ratio * range;
        return 'rotate(' + newAngle + ') translate(0,' + (options.labelInset + options.ringWidth - data.radius) + ')';
      }).style('stroke', 'white').style('stroke-width', '1px');
      lines.exit().remove(); // Text around arc

      text = labels.selectAll('text').data(ticks);
      text.enter().append('text').attr("text-anchor", "middle");

      var tickText = function tickText(d) {
        var selection = d3.select(this);

        if (data.displayTags) {
          selection.text(data.displayTags[d]);
        } else {
          //historically the precision is zero, so only use precision when min/max is small
          var tickPrecision = data.precision;

          if (Math.abs(data.max - data.min) > 10) {
            tickPrecision = 0;
          }

          promises.push(modelUtil.resolveNumericDisplay(d, {
            units: null,
            precision: tickPrecision
          }).then(function (display) {
            selection.text(display);
          }));
        }
      };

      text.attr('transform', function (d) {
        var ratio = scale(d),
            newAngle = options.minAngle + ratio * range;
        return 'rotate(' + newAngle + ') translate(0,' + (options.labelInset - data.radius) + ')';
      }).each(tickText);
      text.exit().remove();
    })();

    (function renderPointer() {
      var lineData, pointerHeadLength, pg, path;
      pointerHeadLength = Math.round(data.radius * options.pointerHeadLengthPercent) - options.ringWidth;
      lineData = [[options.pointerWidth / 2, 0], [0, -pointerHeadLength], [-(options.pointerWidth / 2), 0], [0, options.pointerTailLength], [options.pointerWidth / 2, 0]];
      pg = gauge.selectAll(".pointer").data([lineData]);
      pg.enter().append('g').attr('class', 'pointer').attr("fill", options.defaultShapeColor);
      pg.attr('transform', data.centerTx).transition().attr("fill", data.color ? data.color : options.defaultShapeColor);
      pg.exit().remove();
      path = gauge.select(".pointer").selectAll("path").data(data.value === null || data.value === undefined ? [] : [lineData]);
      path.enter().append('path').attr('d', options.pointerLine).attr('transform', 'rotate(' + options.minAngle + ')'); // Update pointer

      path.attr('d', options.pointerLine).transition().duration(options.transitionMs).attrTween("transform", function tween(d, i, a) {
        // Do custom tween because we want the needle to rotate
        // the correct way.
        var aNum = parseFloat(/rotate\((.+)\)/.exec(a)[1]),
            b = options.minAngle + scale(data.value) * range,
            interp = function interpNum(t) {
          return aNum * (1 - t) + b * t;
        };

        return function (t) {
          var res = interp(t);
          return 'rotate(' + res + ')';
        };
      });
      path.exit().remove();
    })();

    (function renderTitle() {
      var title = gauge.selectAll('.title').data(dataArray);
      title.enter().append("g").attr("class", "title").append("text").attr("text-anchor", "middle");
      title.select("text").attr("transform", function (d) {
        return 'translate(' + d.radius + ',' + (d.radius + options.titleYOffset) + ')';
      }).text(function (d) {
        return d.title;
      });
      title.exit().remove();
    })();

    (function renderValueText() {
      var valueText = gauge.selectAll('.valueText').data(dataArray);
      valueText.enter().append("g").attr("class", "valueText").append("text").attr("text-anchor", "middle");
      valueText.select("text").attr("transform", function (d) {
        return 'translate(' + d.radius + ',' + (d.radius + options.valueTextYOffset) + ')';
      }).text(function (d) {
        return d.valueText;
      });
      valueText.exit().remove();
    })();

    return Promise.all(promises);
  }
  /**
   * Render the gauge. This will resolve the data asynchronously
   * and then render the data in the gauge. Note that any request to render while rendering is in progress
   * will wait for the current rendering to complete before starting a new rendering request
   *
   * @private
   * @inner
   *
   * @returns {Promise}
   */


  CircularGaugeWidget.prototype.$renderData = function () {
    var that = this;
    return model.resolveData(that).then(function (data) {
      return renderData(that, data);
    });
  }; ////////////////////////////////////////////////////////////////
  // Override ORD
  ////////////////////////////////////////////////////////////////

  /**
   * Resolves any override ORD and then renders the widget.
   *
   * @inner
   * @private
   *
   * @param widget The widget instance.
   * @return {Promise|*}
   */


  function resolveOverrideOrd(widget) {
    var ord = widget.properties().getValue(overrideOrdName);

    if (ord) {
      return widget.resolve(ord).then(function (value) {
        //reset gauge statistics on ord modification
        widget.$lastMin = undefined;
        widget.$lastMax = undefined;
        widget.$overrideVal = value;
        return widget.$renderData();
      });
    } else {
      delete widget.$overrideVal;
    }
  }
  /**
   * Called when a new data value is dragged and dropped onto the widget.
   *
   * @inner
   * @private
   *
   * @param  widget The widget instance to update with the new value.
   * @param  dataTransfer The data
   * @return {Promise} A promise that's resolved once the drag and drop operation
   * has completed.
   */


  function updateFromDrop(widget, dataTransfer) {
    return dragDropUtils.fromClipboard(dataTransfer).then(function (envelope) {
      switch (envelope.getMimeType()) {
        case 'niagara/navnodes':
          return envelope.toJson().then(function (json) {
            var obj = json && json[0],
                oldOverrideOrd;

            if (obj && obj.ord) {
              // Record any ORD value so we can unsubscribe it.
              oldOverrideOrd = widget.properties().getValue(overrideOrdName);
              widget.properties().setValue(overrideOrdName, obj.ord);

              if (oldOverrideOrd) {
                return baja.Ord.make(oldOverrideOrd).resolve().then(function (target) {
                  var comp = target.getComponent();

                  if (comp) {
                    return widget.getSubscriber().unsubscribe(comp);
                  }
                });
              }
            }
          }).then(function () {
            return resolveOverrideOrd(widget);
          });
      }
    });
  } ////////////////////////////////////////////////////////////////
  // Widget
  ////////////////////////////////////////////////////////////////


  CircularGaugeWidget.prototype.doInitialize = function (element) {
    var that = this;
    element.addClass("CircularGaugeWidgetOuter");
    var cssClass = 'CircularGaugeWidget';

    if (webChartUtil.isSafari()) {
      cssClass += ' heightFix';
    }

    d3.select(element[0]).append('svg').attr('top', 0).attr('left', 0).attr('width', "100%").attr('height', "100%").attr('class', cssClass).append('g').attr('transform', 'translate(' + options.left + ',' + options.top + ')');
    that.jq().on("dragover", function (e) {
      e.preventDefault();
    }).on("drop", function (e) {
      updateFromDrop(that, e.originalEvent.dataTransfer)["catch"](baja.error);
      e.preventDefault();
      e.stopPropagation();
    });
    that.getSubscriber().attach("changed", function () {
      that.$renderData(that)["catch"](baja.error);
    });
    return resolveOverrideOrd(that);
  };

  CircularGaugeWidget.prototype.doLayout = CircularGaugeWidget.prototype.doChanged = function () {
    return this.$renderData(this);
  };

  CircularGaugeWidget.prototype.doDestroy = function () {
    this.jq().removeClass("CircularGaugeWidgetOuter");
  };

  CircularGaugeWidget.prototype.doLoad = function () {
    return this.$renderData(this);
  };

  return CircularGaugeWidget;
});
